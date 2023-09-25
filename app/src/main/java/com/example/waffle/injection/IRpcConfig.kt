package com.example.waffle.injection

import com.solana.mobilewalletadapter.clientlib.RpcCluster

/**
 * RPC config interface
 */
interface IRpcConfig {

    val solanaRpcUrl: String
    val rpcCluster: RpcCluster
}
