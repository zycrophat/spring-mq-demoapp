package steffan.springmqdemoapp.app.config


import org.apache.activemq.ActiveMQConnectionFactory
import org.infinispan.configuration.cache.ConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfigurationBuilder
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
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.connection.CachingConnectionFactory
import org.springframework.mock.jndi.SimpleNamingContextBuilder
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import javax.transaction.TransactionManager


@Configuration
@EnableTransactionManagement
@EnableJms
@EnableCaching
open class JmsConfiguration {

    @Value("\${spring.activemq.broker-url}")
    val brokerUrl: String? = null

    @Value("\${spring.activemq.user}")
    val user: String? = null

    @Value("\${spring.activemq.password}")
    val pass: String? = null

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

        val builder = SimpleNamingContextBuilder()
        builder.bind("java:comp/env/jdbc/messageIdDataSource", dataSource)
        builder.activate()
        return dataSource
    }

    @Bean
    open fun globalConfigurer(): InfinispanGlobalConfigurer {
        return InfinispanGlobalConfigurer {
            GlobalConfigurationBuilder().transport()
                    .defaultTransport()
                    .defaultCacheName("infini-cache1")
                    .build()
        }
    }

    @Bean
    open fun cacheConfigurer(txManager: TransactionManager): InfinispanCacheConfigurer {
        return InfinispanCacheConfigurer { manager ->
            val config = ConfigurationBuilder()
            config.apply {
                transaction()
                        .transactionMode(TransactionMode.TRANSACTIONAL)
                        .transactionManagerLookup { txManager}
                        //.autoCommit(true)
                        .transactionProtocol(TransactionProtocol.DEFAULT)
                persistence()
                        .addStore(JdbcStringBasedStoreConfigurationBuilder::class.java)
                        .fetchPersistentState(true)
                        .ignoreModifications(false)
                        .purgeOnStartup(false)
                        .shared(true)
                        .table()
                        .dropOnExit(true)
                        .createOnStart(true)
                        .tableNamePrefix("ISPN_STRING_TABLE")
                        .idColumnName("ID_COLUMN").idColumnType("VARCHAR(255)")
                        .dataColumnName("DATA_COLUMN").dataColumnType("VARBINARY(1024)")
                        .timestampColumnName("TIMESTAMP_COLUMN").timestampColumnType("BIGINT")
                        .dataSource().jndiUrl("java:comp/env/jdbc/messageIdDataSource")


                expiration()
                        .lifespan(1, TimeUnit.MINUTES)
            }

            manager.defineConfiguration("local-config", config.build(true));
        }
    }

}
