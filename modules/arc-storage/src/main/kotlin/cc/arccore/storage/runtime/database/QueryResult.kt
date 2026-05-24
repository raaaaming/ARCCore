package cc.arccore.storage.runtime.database

/**
 * Represents the result set returned by a database query.
 *
 * Implementations must be closed after use to release underlying resources
 * (cursors, result sets, etc.).
 */
interface QueryResult : AutoCloseable {

    /** Returns `true` if the result set has at least one row. */
    fun hasRows(): Boolean

    /** Advances to the next row. Returns `false` when exhausted. */
    fun next(): Boolean

    /**
     * Returns the string value of the column identified by [columnName],
     * or `null` if the column is NULL.
     */
    fun getString(columnName: String): String?

    /**
     * Returns the integer value of [columnName], or `null` if NULL.
     */
    fun getInt(columnName: String): Int?

    /**
     * Returns the long value of [columnName], or `null` if NULL.
     */
    fun getLong(columnName: String): Long?

    /**
     * Returns the double value of [columnName], or `null` if NULL.
     */
    fun getDouble(columnName: String): Double?

    /**
     * Returns the boolean value of [columnName], or `null` if NULL.
     */
    fun getBoolean(columnName: String): Boolean?

    /** Releases the resources held by this result. Idempotent. */
    override fun close()
}
