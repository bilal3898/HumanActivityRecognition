package com.bilal.humanactivityrecognition.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Abstraction over the inference runtime so the classifier can be unit-tested without TFLite. */
interface InferenceEngine : AutoCloseable {
    /** input: [1][128][9] → returns 6 class probabilities. */
    fun run(input: Array<Array<FloatArray>>): FloatArray
}

class TensorFlowLiteHelper(context: Context, modelPath: String, numThreads: Int = 2) : InferenceEngine {
    private val interpreter: Interpreter
    val inputShape: IntArray
    val outputShape: IntArray

    init {
        val options = Interpreter.Options().setNumThreads(numThreads)
        interpreter = Interpreter(loadModelFile(context, modelPath), options)
        inputShape = interpreter.getInputTensor(0).shape()
        outputShape = interpreter.getOutputTensor(0).shape()
    }

    override fun run(input: Array<Array<FloatArray>>): FloatArray {
        val output = Array(1) { FloatArray(outputShape.last()) }
        synchronized(interpreter) { interpreter.run(input, output) }
        return output[0]
    }

    override fun close() = interpreter.close()

    companion object {
        fun loadModelFile(context: Context, path: String): MappedByteBuffer =
            context.assets.openFd(path).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
    }
}
