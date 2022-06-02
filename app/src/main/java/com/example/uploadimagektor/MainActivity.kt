package com.example.uploadimagektor

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.uploadimagektor.databinding.ActivityMainBinding
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var uri:Uri
    private lateinit var fileName:String
    lateinit var fileExt:String
    lateinit var bytesArray:ByteArray

    private val requestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
        if (it){
            Toast.makeText(this, "Permiso otorgado", Toast.LENGTH_SHORT).show()
        }
    }
    val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if (it.resultCode == Activity.RESULT_OK){
            if (it.data != null){
                uri= it.data!!.data as Uri
                val thumbnail: Bitmap =
                    applicationContext.contentResolver.loadThumbnail(
                        uri, Size(640, 480), null)
                binding.img.setImageBitmap(thumbnail)


                val resolver = applicationContext.contentResolver
                val stream = resolver.openInputStream(uri)
                val query = contentResolver.query(uri,null,null,null,null)
                query.use {cursor->
                    cursor?.moveToFirst()
                    val nameColumn = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val extColumn = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                    if (nameColumn != null && extColumn != null){
                        fileName = cursor.getString(nameColumn)
                        fileExt = cursor.getString(extColumn)
                        Toast.makeText(this,"$fileName $fileExt", Toast.LENGTH_SHORT).show()
                    }

                }
                bytesArray = stream?.readBytes() ?: throw Exception("bytes array null")
                binding.btnUpload.visibility = View.VISIBLE
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        askPermission()
        binding.btnPickImage.setOnClickListener {
            val i = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getResult.launch(i)
        }
        binding.btnUpload.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {

                    try {
                        KtorClient.httpClient.submitFormWithBinaryData(
                            url = "https://twitter-z.herokuapp.com/posts",
                            formData = formData {
                                append("content",binding.tweet.text.toString())
                                    for (i in 0..1){
                                        append(
                                            "images",
                                            bytesArray,
                                            Headers.build {
                                                append(HttpHeaders.ContentType,fileExt)
                                                append(HttpHeaders.ContentDisposition,"filename=$fileName")
                                            }
                                        )
                                    }

                            }
                        )
                    }catch (e:Exception){
                       withContext(Dispatchers.Main){
                           Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                       }
                    }
                }
            }
    }
    private fun askPermission(){
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> {

            }
            else -> {
                requestLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}