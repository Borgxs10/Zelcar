package com.zelcar.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Sincronização com a nuvem (RF11/RNF: "sobem para a nuvem em segundo plano").
 * O protótipo não define a API do backend, então esta implementação é local; troque por
 * uma implementação de rede (Retrofit/Ktor) mantendo a interface.
 */
interface SyncService {
    suspend fun sync(): Result<Unit>
}

class LocalSyncService : SyncService {
    override suspend fun sync(): Result<Unit> = runCatching { delay(1200) }
}

/** Ingresso de transferência: o QR carrega só um token de uso único, que expira em 10 min (RN09). */
data class TransferTicket(val token: String, val uri: String, val ttlSeconds: Int)

interface TransferService {
    suspend fun createTicket(carId: String, blocks: Set<String>): TransferTicket
}

class LocalTransferService : TransferService {
    override suspend fun createTicket(carId: String, blocks: Set<String>): TransferTicket {
        val token = UUID.randomUUID().toString()
        val uri = "zelcar://transfer?token=$token&blocks=${blocks.sorted().joinToString(",")}"
        return TransferTicket(token, uri, ttlSeconds = 600)
    }
}

/** Copia fotos escolhidas pelo usuário para o armazenamento privado do app, reduzidas a no máx. 1600 px. */
object PhotoStore {
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val bmp = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@withContext null
            val dir = File(context.filesDir, "photos").apply { mkdirs() }
            val out = File(dir, UUID.randomUUID().toString() + ".jpg")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun decodeThumb(path: String, target: Int = 160): Bitmap? = try {
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, b)
        var s = 1
        while (b.outWidth / s > target * 2 || b.outHeight / s > target * 2) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    } catch (e: Exception) {
        null
    }
}
