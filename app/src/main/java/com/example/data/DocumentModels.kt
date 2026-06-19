package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ROOM ENTITIES ---

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "PDF", "Scan", "ID Card", "Blank Note", "Lined Note"
    val timestamp: Long = System.currentTimeMillis(),
    val pageCount: Int = 1,
    val isStarred: Boolean = false,
    val fileUri: String = "", // Real local path on device to export/share
    val contentJson: String = "", // Comprehensive page-by-page annotation schema (JSON)
    val isSaved: Boolean = false
)

@Entity(tableName = "signatures")
data class SignatureProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alias: String,
    val pathDataJson: String, // Stringified coordinates for restoring smooth vector drawing
    val colorHex: String = "#000000",
    val strokeWidth: Float = 8f,
    val timestamp: Long = System.currentTimeMillis()
)

// --- ROOM DAOs ---

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Delete
    suspend fun deleteDocument(document: Document)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY timestamp DESC")
    fun getAllSignatures(): Flow<List<SignatureProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureProfile): Long

    @Delete
    suspend fun deleteSignature(signature: SignatureProfile)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteSignatureById(id: Int)
}

// --- APP DATABASE ---

@Database(entities = [Document::class, SignatureProfile::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun signatureDao(): SignatureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "acropdf_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- DATA REPOSITORY ---

class DocumentRepository(private val db: AppDatabase) {
    val allDocuments: Flow<List<Document>> = db.documentDao().getAllDocuments()
    val starredDocuments: Flow<List<Document>> = db.documentDao().getStarredDocuments()
    val allSignatures: Flow<List<SignatureProfile>> = db.signatureDao().getAllSignatures()

    suspend fun getDocumentById(id: Int): Document? = db.documentDao().getDocumentById(id)

    suspend fun insertDocument(document: Document): Long = db.documentDao().insertDocument(document)

    suspend fun updateDocument(document: Document) = db.documentDao().updateDocument(document)

    suspend fun deleteDocument(document: Document) = db.documentDao().deleteDocument(document)

    suspend fun deleteDocumentById(id: Int) = db.documentDao().deleteDocumentById(id)

    suspend fun insertSignature(signature: SignatureProfile): Long = db.signatureDao().insertSignature(signature)

    suspend fun deleteSignature(signature: SignatureProfile) = db.signatureDao().deleteSignature(signature)
    
    suspend fun deleteSignatureById(id: Int) = db.signatureDao().deleteSignatureById(id)
}
