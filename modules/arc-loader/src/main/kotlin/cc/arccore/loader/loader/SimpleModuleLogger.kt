package cc.arccore.loader.loader

import cc.arccore.api.module.ModuleLogger
import java.util.logging.Level
import java.util.logging.Logger

class SimpleModuleLogger(
    private val moduleId: String
) : ModuleLogger {

    private val logger: Logger = Logger.getLogger("ARCCore.Module.$moduleId")

    override fun info(message: String) {
        logger.log(Level.INFO, "[$moduleId] $message")
    }

    override fun warn(message: String) {
        logger.log(Level.WARNING, "[$moduleId] $message")
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable != null) {
            logger.log(Level.SEVERE, "[$moduleId] $message", throwable)
        } else {
            logger.log(Level.SEVERE, "[$moduleId] $message")
        }
    }

    override fun debug(message: String) {
        logger.log(Level.FINE, "[$moduleId] $message")
    }

    override fun trace(message: String) {
        logger.log(Level.FINER, "[$moduleId] $message")
    }

    override fun withPrefix(prefix: String): ModuleLogger {
        return PrefixedModuleLogger(moduleId, prefix)
    }

    private class PrefixedModuleLogger(
        private val moduleId: String,
        private val prefix: String
    ) : ModuleLogger {

        private val logger: Logger = Logger.getLogger("ARCCore.Module.$moduleId")

        override fun info(message: String) {
            logger.log(Level.INFO, "[$moduleId][$prefix] $message")
        }

        override fun warn(message: String) {
            logger.log(Level.WARNING, "[$moduleId][$prefix] $message")
        }

        override fun error(message: String, throwable: Throwable?) {
            if (throwable != null) {
                logger.log(Level.SEVERE, "[$moduleId][$prefix] $message", throwable)
            } else {
                logger.log(Level.SEVERE, "[$moduleId][$prefix] $message")
            }
        }

        override fun debug(message: String) {
            logger.log(Level.FINE, "[$moduleId][$prefix] $message")
        }

        override fun trace(message: String) {
            logger.log(Level.FINER, "[$moduleId][$prefix] $message")
        }

        override fun withPrefix(prefix: String): ModuleLogger {
            return PrefixedModuleLogger(moduleId, "${this.prefix}.$prefix")
        }
    }
}
