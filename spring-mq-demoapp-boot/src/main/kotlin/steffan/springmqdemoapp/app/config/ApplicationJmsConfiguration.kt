package steffan.springmqdemoapp.app.config


import org.apache.activemq.ActiveMQXAConnectionFactory
import org.apache.activemq.RedeliveryPolicy
import org.apache.camel.component.jms.JmsConfiguration
import org.h2.jdbcx.JdbcDataSource
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder
import org.infinispan.spring.starter.embedded.InfinispanCacheConfigurer
import org.infinispan.spring.starter.embedded.InfinispanGlobalConfigurer
import org.infinispan.transaction.TransactionMode
import org.infinispan.transaction.TransactionProtocol
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.AdviceMode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.mock.jndi.SimpleNamingContextBuilder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import steffan.springmqdemoapp.routes.TypeConvertingGreetingRequestProcessor
import steffan.springmqdemoapp.routes.UnmarshalledGreetingRequestProcessor
import java.util.concurrent.TimeUnit
import javax.jms.ConnectionFactory
import javax.sql.XADataSource
import javax.transaction.TransactionManager


@Configuration
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableJms
@EnableCaching
open class ApplicationJmsConfiguration {

    @Value("\${spring.activemq.broker-url}")
    lateinit var brokerUrl: String

    @Value("\${spring.activemq.user}")
    lateinit var user: String

    @Value("\${spring.activemq.password}")
    lateinit var pass: String

    @Value("\${app.camel.datasource.jndiName}")
    lateinit var messageIdDataSourceJndiName: String

    @Value("\${app.camel.datasource.jdbc-url}")
    lateinit var messageIdDataSourceUrl: String

    @Bean
    open fun connectionFactory(): ConnectionFactory {
        return CachingConnectionFactory(AtomikosConnectionFactoryBean().apply {
            xaConnectionFactory = ActiveMQXAConnectionFactory().apply {
                brokerURL = brokerUrl
                userName = user
                password = pass
                redeliveryPolicy = redeliveryPolicy()
            }
            setPoolSize(1)
            uniqueResourceName = "activemqConnectionFactory"
        })
    }

    @Bean
    open fun redeliveryPolicy(): RedeliveryPolicy {
        return RedeliveryPolicy().apply {
            initialRedeliveryDelay = 500L
            backOffMultiplier = 1.5
            isUseExponentialBackOff = true
            maximumRedeliveries = 2
        }
    }

    @Bean
    open fun jmsConfiguration(txManager: PlatformTransactionManager): JmsConfiguration {
        return JmsConfiguration().apply {
            connectionFactory = connectionFactory()
            transactionManager = txManager
            isTransacted = true
            cacheLevelName = "CACHE_CONNECTION"
        }
    }

    @Bean
    open fun jmsListenerContainerFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer):
            JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, connectionFactory())
        factory.setSessionTransacted(true)
        return factory
    }

    @Bean
    open fun messageIdDataSource(): XADataSource {
        val dataSource = DataSourceBuilder
                .create()
                .type(JdbcDataSource::class.java)
                .url(messageIdDataSourceUrl)
                .build()
        dataSource.description = "messageIdDataSource"

        SimpleNamingContextBuilder().apply {
            bind(messageIdDataSourceJndiName, dataSource)
            activate()
        }
        return dataSource
    }

    @Bean
    open fun infinispanGlobalConfigurerlobalConfigurer(): InfinispanGlobalConfigurer {
        return InfinispanGlobalConfigurer {
            GlobalConfigurationBuilder().transport()
                    .defaultTransport()
                    .defaultCacheName("defaultCache")
                    .build()
        }
    }

    @Bean
    @DependsOn("messageIdDataSource")
    open fun infinispanCacheConfigurer(txManager: TransactionManager): InfinispanCacheConfigurer {
        return InfinispanCacheConfigurer { manager ->
            val config = ConfigurationBuilder()
            config.apply {
                transaction()
                        .transactionMode(TransactionMode.TRANSACTIONAL)
                        .transactionManagerLookup { txManager }
                        .autoCommit(false)
                        .transactionProtocol(TransactionProtocol.DEFAULT)
                        .recovery()
                            .enable()

                persistence()
                        .passivation(false)
                        .addStore(JdbcStringBasedStoreConfigurationBuilder::class.java)
                        .async().disable()
                        .ignoreModifications(false)
                        .fetchPersistentState(false)
                        .purgeOnStartup(false)
                        .shared(true)
                        .table()
                            .dropOnExit(false)
                            .createOnStart(true)
                            .tableNamePrefix("ISPN_STRING_TABLE")
                            .idColumnName("ID_COLUMN").idColumnType("VARCHAR(255)")
                            .dataColumnName("DATA_COLUMN").dataColumnType("VARBINARY(1024)")
                            .timestampColumnName("TIMESTAMP_COLUMN").timestampColumnType("BIGINT")
                        .dataSource()
                            .jndiUrl(messageIdDataSourceJndiName)

                expiration()
                        .lifespan(1, TimeUnit.MINUTES)
                        .enableReaper()
            }

            manager.defineConfiguration(UnmarshalledGreetingRequestProcessor::class.simpleName, config.build(true))
            manager.defineConfiguration(TypeConvertingGreetingRequestProcessor::class.simpleName, config.build(true))
        }
    }

}
