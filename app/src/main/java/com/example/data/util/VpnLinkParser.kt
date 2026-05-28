package com.example.data.util

import android.util.Base64
import com.example.data.local.VpnProfileEntity
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

object VpnLinkParser {

    fun parseSubscriptionContent(content: String, subscriptionId: Int? = null): List<VpnProfileEntity> {
        val result = mutableListOf<VpnProfileEntity>()
        if (content.isBlank()) return result

        val decodedLines = try {
            val cleaned = content.trim().replace("\r", "")
            val decodedBytes = Base64.decode(cleaned, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8).split("\n")
        } catch (e: Exception) {
            content.split("\n")
        }

        for (rawLine in decodedLines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            try {
                val profile = parseSingleLink(line, subscriptionId)
                if (profile != null) {
                    result.add(profile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    fun parseSingleLink(link: String, subscriptionId: Int? = null): VpnProfileEntity? {
        if (!link.contains("://")) return null

        val scheme = link.substringBefore("://").lowercase()
        return when (scheme) {
            "vless" -> parseGenericLink(link, "VLESS", subscriptionId)
            "vmess" -> parseVmessLink(link, subscriptionId)
            "ss" -> parseGenericLink(link, "SHADOWSOCKS", subscriptionId)
            "trojan" -> parseGenericLink(link, "TROJAN", subscriptionId)
            else -> null
        }
    }

    private fun parseVmessLink(link: String, subscriptionId: Int?): VpnProfileEntity? {
        return try {
            val rawPayload = link.substringAfter("vmess://").trim()
            val decodedPayload = String(Base64.decode(rawPayload, Base64.DEFAULT), Charsets.UTF_8)
            val json = JSONObject(decodedPayload)
            
            val server = json.optString("add", "unknown")
            val port = json.optInt("port", 443)
            val name = json.optString("ps", "VMess Server")
            
            VpnProfileEntity(
                subscriptionId = subscriptionId,
                name = name,
                server = server,
                port = port,
                protocol = "VMESS",
                configPayload = link,
                countryCode = extractCountryCode(name)
            )
        } catch (e: Exception) {
            VpnProfileEntity(
                subscriptionId = subscriptionId,
                name = "VMess Dynamic",
                server = "127.0.0.1",
                port = 443,
                protocol = "VMESS",
                configPayload = link,
                countryCode = "UN"
            )
        }
    }

    private fun parseGenericLink(link: String, protocol: String, subscriptionId: Int?): VpnProfileEntity? {
        return try {
            val uri = URI(link)
            val host = uri.host ?: "127.0.0.1"
            val port = if (uri.port != -1) uri.port else 443
            val name = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "$protocol Server"
            
            VpnProfileEntity(
                subscriptionId = subscriptionId,
                name = name,
                server = host,
                port = port,
                protocol = protocol,
                configPayload = link,
                countryCode = extractCountryCode(name)
            )
        } catch (e: Exception) {
            try {
                val label = link.substringAfter("#", "").let { 
                    if (it.isNotEmpty()) URLDecoder.decode(it, "UTF-8") else "$protocol Server" 
                }
                val cleanLink = link.substringBefore("#").substringAfter("://")
                val addressPart = cleanLink.substringAfter("@", cleanLink)
                val host = addressPart.substringBefore(":")
                val portStr = addressPart.substringAfter(":", "443").substringBefore("?")
                val port = portStr.toIntOrNull() ?: 443

                VpnProfileEntity(
                    subscriptionId = subscriptionId,
                    name = label,
                    server = host,
                    port = port,
                    protocol = protocol,
                    configPayload = link,
                    countryCode = extractCountryCode(label)
                )
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun extractCountryCode(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("germany") || lower.contains("de") || lower.contains("🇩🇪") -> "DE"
            lower.contains("usa") || lower.contains("us") || lower.contains("🇺🇸") -> "US"
            lower.contains("finland") || lower.contains("fi") || lower.contains("🇫🇮") -> "FI"
            lower.contains("singapore") || lower.contains("sg") || lower.contains("🇸🇬") -> "SG"
            lower.contains("russia") || lower.contains("ru") || lower.contains("🇷🇺") -> "RU"
            lower.contains("japan") || lower.contains("jp") || lower.contains("🇯🇵") -> "JP"
            lower.contains("turkey") || lower.contains("tr") || lower.contains("🇹🇷") -> "TR"
            lower.contains("netherlands") || lower.contains("nl") || lower.contains("🇳🇱") -> "NL"
            lower.contains("france") || lower.contains("fr") || lower.contains("🇫🇷") -> "FR"
            else -> "UN"
        }
    }
}
