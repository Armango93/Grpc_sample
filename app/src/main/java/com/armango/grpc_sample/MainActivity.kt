package com.armango.grpc_sample

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.examples.helloworld.GreeterGrpc
import io.grpc.examples.helloworld.HelloReply
import io.grpc.examples.helloworld.HelloRequest
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

//!! Download, compile and run sample server from here https://grpc.io/docs/quickstart/android/
//on your local machine and change BASE_PORT and BASE_ADDRESS
// to port and host of your local running copy of server !!
private const val BASE_PORT: Int = 50051
private const val BASE_ADDRESS = "192.168.0.103"

class MainActivity : AppCompatActivity() {

    var job: Job? = null

    var textWatcher = object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {}
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            hideResponse()
        }
    }

    private val helloClickListener = View.OnClickListener {
        hideResponse()
        if (enterNameInput.text.isNotBlank()) {
            showProgress()
            job = GlobalScope.launch(Dispatchers.Main) {
                try {
                    val resp = requestClient.sayHello(
                        HelloRequest.newBuilder().setName(enterNameInput.text.toString()).build()
                    )
                    hideProgress()
                    showResponse(resp)
                } catch (e: Exception) {
                    hideProgress()
                    showErrorToast(e)
                }
            }
        } else {
           showErrorToast()
        }
    }

    private lateinit var requestClient: GreeterGrpc.GreeterBlockingStub
    private lateinit var channel: ManagedChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initRequests()

        sayHelloBtn.setOnClickListener(helloClickListener)
        enterNameInput.addTextChangedListener(textWatcher)
    }

    private fun initRequests() {
        channel = ManagedChannelBuilder
            .forAddress(BASE_ADDRESS, BASE_PORT)
            .usePlaintext()
            .build()

        requestClient = GreeterGrpc.newBlockingStub(channel)
    }

    private fun showResponse(reply: HelloReply) {
        responseText.visibility = View.VISIBLE
        responseText.text = reply.message
    }

    private fun hideResponse() {
        responseText.visibility = View.GONE
    }

    private fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progress.visibility = View.GONE
    }

    private fun showErrorToast(t: Throwable? = null) {
        if (t != null){
            Toast.makeText(
                this@MainActivity,
                t.message,
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.emptyInputError),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sayHelloBtn.setOnClickListener(null)
        enterNameInput.removeTextChangedListener(textWatcher)

        job?.cancelChildren()
        channel.shutdownNow()
    }
}