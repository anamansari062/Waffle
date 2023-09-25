package com.example.waffle.usecase

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.waffle.data.getLatestBlockhash
import com.example.waffle.repository.SendTransactionRepository
import com.solana.Solana
import com.solana.core.AccountMeta
import com.solana.core.PublicKey
import com.solana.core.SerializeConfig
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.networking.serialization.format.Borsh
import com.solana.networking.serialization.serializers.solana.AnchorInstructionSerializer
import com.solana.programs.SystemProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

class WaffleUseCase @Inject constructor(
    private val walletAdapter: MobileWalletAdapter,
    private val persistenceUseCase: WalletConnectionUseCase,
    private val sendTransactionRepository: SendTransactionRepository,
) {


    private suspend fun getWalletConnection(scope: CoroutineScope): Connected {
        return persistenceUseCase.walletDetails.stateIn(scope).value as Connected
    }

    private fun makeWaffleObject(name: String, user: PublicKey): TransactionInstruction {
        // Derive the waffle PDA from the name
        val waffleKey = PublicKey.findProgramAddress(
            listOf("waffle".toByteArray(), name.toByteArray()),
            PublicKey("B6cnkQKZeNT4VEmkjfNfisL4UzobUfL5wU93xi16DSTU")
        )

        // Defining all accounts involved in the instruction
        val keys = mutableListOf<AccountMeta>()
        keys.add(AccountMeta(waffleKey.address, false, true))
        keys.add(AccountMeta(user, true, true))
        keys.add(AccountMeta(SystemProgram.PROGRAM_ID, false, false))


        return TransactionInstruction(
            PublicKey("B6cnkQKZeNT4VEmkjfNfisL4UzobUfL5wU93xi16DSTU"),
            keys,
            Borsh.encodeToByteArray(
                AnchorInstructionSerializer("create_waffle"),
                Args_createWaffle(name)
            )
        )

    }

    @Serializable
    class Args_createWaffle(val name: String)


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createWaffle(
        identityUri: Uri,
        iconUri: Uri,
        identityName: String,
        sender: ActivityResultSender,
        user: PublicKey,
        solana: Solana,
        waffle: String
    ) = withContext(Dispatchers.IO) {

        val currConn = getWalletConnection(this)
        val authToken = currConn.authToken

        try {
            val blockhash = solana.api.getLatestBlockhash().getOrThrow()
            val transaction = Transaction()
            transaction.setRecentBlockHash(blockhash)
            transaction.addInstruction(makeWaffleObject(waffle, user))
            transaction.feePayer = user

            val transferResult = walletAdapter.transact(sender) {
                val reauth = reauthorize(identityUri, iconUri, identityName, authToken)
                persistenceUseCase.persistConnection(
                    reauth.publicKey,
                    reauth.accountLabel ?: "",
                    reauth.authToken
                )
                val signingResult = signTransactions(
                    arrayOf(
                        transaction.serialize(
                            SerializeConfig(
                                requireAllSignatures = false,
                                verifySignatures = false
                            )
                        )
                    )
                )
                return@transact Transaction.from(signingResult.signedPayloads[0])
            }

            when (transferResult) {
                is TransactionResult.Success -> {
                    try {
                        val transferId =
                            sendTransactionRepository.sendTransaction(transferResult.payload)
                                .getOrThrow()
                        sendTransactionRepository.confirmTransaction(transferId)
                        Log.d(ContentValues.TAG, "https://solana.fm/tx/$transferId?cluster=devnet-solana")
                    } catch (e: Exception) {
                        Log.d(ContentValues.TAG, "Transaction sending failed: ${e.message.toString()}")
                        return@withContext
                    }
                }
                is TransactionResult.Failure -> {
                    return@withContext
                }
                else -> {}
            }

        }
        catch (e: Exception){
            Log.d(ContentValues.TAG, "Blockhash fetch failed: ${e.message.toString()}")
            return@withContext
        }

    }
}
