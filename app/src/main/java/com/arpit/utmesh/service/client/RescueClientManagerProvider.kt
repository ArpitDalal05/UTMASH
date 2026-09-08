package com.arpit.utmesh.service.client

interface RescueClientManagerProvider {
    fun getManager(): BleClientManager
}
