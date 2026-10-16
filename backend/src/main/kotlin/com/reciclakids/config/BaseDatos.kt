package com.reciclakids.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

/**
 * Conexión a PostgreSQL (ADR-03): pool Hikari, migraciones Flyway al arrancar y Exposed como ORM.
 * En Cloud Run la URL puede usar el conector de Cloud SQL (ver docs/despliegue.md).
 */
class BaseDatos private constructor(private val fuente: HikariDataSource) : AutoCloseable {

    val database: Database = Database.connect(fuente)

    /** Corre [bloque] en una transacción, fuera del hilo de las peticiones. */
    suspend fun <T> transaccion(bloque: JdbcTransaction.() -> T): T = withContext(Dispatchers.IO) {
        transaction(database, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED) { bloque() }
    }

    override fun close() = fuente.close()

    companion object {
        fun conectar(config: ConfiguracionBaseDatos): BaseDatos {
            val hikari = HikariConfig().apply {
                jdbcUrl = config.url
                username = config.usuario
                password = config.contrasena
                maximumPoolSize = config.maxConexiones
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_READ_COMMITTED"
                poolName = "reciclakids"
                validate()
            }
            val fuente = HikariDataSource(hikari)
            Flyway.configure()
                .dataSource(fuente)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            return BaseDatos(fuente)
        }
    }
}
