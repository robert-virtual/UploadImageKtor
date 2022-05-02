package com.example.uploadimagektor

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val httpClient = HttpClient(CIO)
    var file:File? = null

    val requestLauncher = registerForActivityResult(
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
                val uri:Uri = it.data!!.data as Uri
                val thumbnail: Bitmap =
                    applicationContext.contentResolver.loadThumbnail(
                        uri, Size(640, 480), null)
                binding.img.setImageBitmap(thumbnail)
                file = it.data!!.data?.encodedPath?.let { it1 -> File(it1) }
                if (file != null){
                    binding.btnUpload.visibility = View.VISIBLE
                }
                Toast.makeText(this, "${file?.name}.${file?.extension}", Toast.LENGTH_SHORT).show()
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
            if (file != null){
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        httpClient.submitFormWithBinaryData(
                            url = "https://twitter-z.herokuapp.com/posts",
                            formData = formData {
                                append("content",binding.tweet.text.toString())
                                append(
                                    "images",
                                    file!!.readBytes(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType,"image/jpg")
                                        append(HttpHeaders.ContentDisposition,"filename=${file!!.name}.jpg")
                                    }
                                )

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
    }
    fun askPermission(){
        when{
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED->{

            }
            else->{
                requestLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}