package steffan.springmqdemoapp.app.config


import org.apache.activemq.ActiveMQConnectionFactory
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.cache.StorageType
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.eviction.EvictionStrategy
import org.infinispan.eviction.EvictionType
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder
import org.infinispan.spring.starter.embedded.InfinispanCacheConfigurer
import org.infinispan.spring.starter.embedded.InfinispanGlobalConfigurer
import org.infinispan.transaction.TransactionMode
import org.infinispan.transaction.TransactionProtocol
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.mock.jndi.SimpleNamingContextBuilder
import org.springframework.transaction.annotation.EnableTransactionManagement
import steffan.springmqdemoapp.routes.UnmarshalledGreetingRequestProcessor
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import javax.transaction.TransactionManager


@Configuration
@EnableTransactionManagement
@EnableJms
@EnableCaching
open class JmsConfiguration {

    @Value("\${spring.activemq.broker-url}")
    lateinit var brokerUrl: String

    @Value("\${spring.activemq.user}")
    lateinit var user: String

    @Value("\${spring.activemq.password}")
    lateinit var pass: String

    @Value("\${app.camel.datasource.jndiName}")
    lateinit var messageIdDataSourceJndiName: String

    @Bean
    open fun activeMQConnectionFactory(): ActiveMQConnectionFactory {
        val activeMQConnectionFactory = ActiveMQConnectionFactory().apply {
            brokerURL = brokerUrl
            userName = user
            password = pass
        }

        return activeMQConnectionFactory
    }

    @Bean
    open fun cachingConnectionFactory(): CachingConnectionFactory {
        return CachingConnectionFactory(activeMQConnectionFactory())
    }

    @Bean
    open fun jmsListenerContainerFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer):
            JmsListenerContainerFactory<*> {
        val factory = DefaultJmsListenerContainerFactory()
        configurer.configure(factory, cachingConnectionFactory())
        return factory
    }

    @Bean
    @ConfigurationProperties(prefix = "app.camel.datasource")
    open fun messageIdDataSource(): DataSource {
        val dataSource = DataSourceBuilder.create().build()

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

            manager.defineConfiguration(UnmarshalledGreetingRequestProcessor::class.simpleName, config.build(true));
        }
    }

}
