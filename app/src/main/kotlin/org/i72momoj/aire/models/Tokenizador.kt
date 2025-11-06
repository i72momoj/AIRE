package org.i72momoj.aire.models

import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class Tokenizador(vocab: InputStream) {
    private var id2token: HashMap<Int, String> = HashMap()
    private var token2id: HashMap<String, Int> = HashMap()

    init {
        cargarVocab(vocab)
    }

    private fun cargarVocab(vocab: InputStream) {
        try {
            val lector = BufferedReader(InputStreamReader(vocab))

            var indice = 0
            var linea: String?

            do {
                linea = lector.readLine()
                id2token[indice] = linea
                token2id[linea] = indice

                indice++
            }
            while (linea != null)

            lector.close()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getTokens(palabra: String, primerToken: Boolean): ArrayList<String> {
        val tokens : ArrayList<String> = ArrayList()
        var token1 = palabra
        var token2 = ""
        var contador = 0

        while(
            ( (token2id.getOrDefault(token1, -1)  == -1 && primerToken) ||
            (token2id.getOrDefault("##$token1", -1) == -1 && !primerToken) ) &&
            contador < palabra.length
        ) {
            token1 = palabra.substring(0, palabra.length - contador)
            token2 = palabra.substring(palabra.length - contador)
            contador++
            Log.i("Tokenización", "Token 1: $token1, Token 2: $token2")
            Log.i("Tokenización", "ID token 1: ${token2id.getOrDefault(token1, -1)}")
        }

        // Si se ha encontrado token antes de acabar la palabra, se devuelve
        if (contador < palabra.length) {
            tokens.add(token1)

            // Si todavía quedan tokens, se obtienen de forma recursiva
            if(token2 != "")
                tokens.addAll(getTokens(token2, false))

            return tokens
        }
        // En caso contrario, el token no está en el vocabulario del modelo
        else
            return arrayListOf("[UNK]")
    }

    fun tokenizar(input: String): ArrayList<Int> {
        val palabras: List<String> = input.lowercase().split(" ")
        var size: Int = palabras.size + 2
        val ids = ArrayList<Int>(size)

        ids.add(token2id.getOrDefault("[CLS]", 101))

        // Obtenemos los IDs de los tokens
        for(palabra in palabras) {
            val tokens: ArrayList<String> = getTokens(palabra, true)

            Log.d("Tokenizador", "tokens extraidos -> ${tokens.toString()}")

            size += tokens.size - 1
            ids.ensureCapacity(size)

            var primertoken = true

            for(token in tokens) {
                //Log.d("Token ID", "token: $token")
                if (primertoken) {
                    //Log.d("Token ID", "token: $token")
                    ids.add(token2id.getOrDefault(token, token2id.getOrDefault("[UNK]", 100)))
                    primertoken = false
                } else
                    ids.add(token2id.getOrDefault("##$token", token2id.getOrDefault("[UNK]", 100)))

                //for(token in tokens)
                //    ids.add(token2id.getOrDefault(token, token2id.getOrDefault("[UNK]", 100)))
            }
        }
        ids.add(token2id.getOrDefault("[SEP]", 102))
        return ids
    }

    fun decode(id: Int): String {
        return id2token.getOrDefault(id, "[UNK]")
    }
}
