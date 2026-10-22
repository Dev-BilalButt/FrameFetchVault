package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify app name and isolated vault partition initialization`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FrameFetch Vault", appName)

        val db = VaultDatabase.getInstance(context)
        val repository = VaultRepository(context, db.vaultVideoDao())
        val vaultDir = repository.getIsolatedVaultDirectory()

        assertTrue(vaultDir.exists())
        assertTrue(File(vaultDir, ".nomedia").exists())
    }
}
