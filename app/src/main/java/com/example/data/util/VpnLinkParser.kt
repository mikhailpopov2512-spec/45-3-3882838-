package com.example.data.util

import android.util.Base64
import com.example.data.local.VpnProfileEntity
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URI

object VpnLinkParser {

    fun parseSubscriptionContent(content: String, subscriptionUrl: String? = null): List<VpnProfileEntity> {
        val decoded = try {
            val cleanContent = content.trim().replace("\r", "").replace("\n", "")
            val decodedBytes = Base64.decode(cleanContent, Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            content
        }

        val profiles = mutableListOf<VpnProfileEntity>()
        val lines = decoded.split("\n", "\r")
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty()) {
                val profile = parseSingleLink(trimmedLine, subscriptionUrl)
                if (profile != null) {
                    profiles.add(profile)
                }
            }
        }
        return profiles
    }

    fun parseSingleLink(link: String, subscriptionUrl: String? = null): VpnProfileEntity? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseUriCompatible(trimmed, "VLESS", subscriptionUrl)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed, subscriptionUrl)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed, subscriptionUrl)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseUriCompatible(trimmed, "TROJAN", subscriptionUrl)
            trimmed.startsWith("hysteria2://", ignoreCase = true) || trimmed.startsWith("hy2://", ignoreCase = true) -> parseUriCompatible(trimmed, "HYSTERIA2", subscriptionUrl)
            trimmed.startsWith("tuic://", ignoreCase = true) -> parseUriCompatible(trimmed, "TUIC", subscriptionUrl)
            else -> null
        }
    }

    private fun parseUriCompatible(link: String, protocol: String, subscriptionUrl: String?): VpnProfileEntity? {
        return try {
            val uri = URI(link)
            val host = uri.host ?: "unknown"
            val port = if (uri.port != -1) uri.port else 443
            
            var name = ""
            if (link.contains("#")) {
                val rawName = link.substringAfter("#")
                name = try {
                    URLDecoder.decode(rawName, "UTF-8")
                } catch (e: Exception) {
                    rawName
                }
            }
            if (name.isEmpty()) name = "$protocol-$host:$port"

            VpnProfileEntity(
                name = name,
                protocol = protocol,
                host = host,
                port = port,
                fullConfig = link,
                subscriptionUrl = subscriptionUrl
            )
        } catch (e: Exception) {
            try {
                val withoutProtocol = link.substringAfter("://")
                val hashParts = withoutProtocol.split("#")
                val name = if (hashParts.size > 1) URLDecoder.decode(hashParts[1], "UTF-8") else "Node"
                val mainPart = hashParts[0]
                val atIndex = mainPart.lastIndexOf("@")
                val hostPort = if (atIndex != -1) mainPart.substring(atIndex + 1) else mainPart
                val colonIndex = hostPort.lastIndexOf(":")
                val host = if (colonIndex != -1) hostPort.substring(0, colonIndex).split("?")[0] else hostPort.split("?")[0]
                val portStr = if (colonIndex != -1) hostPort.substring(colonIndex + 1).split("?")[0] else "443"
                val port = portStr.toIntOrNull() ?: 443

                VpnProfileEntity(
                    name = name,
                    protocol = protocol,
                    host = host,
                    port = port,
                    fullConfig = link,
                    subscriptionUrl = subscriptionUrl
                )
            } catch (ex: Exception) {
                null
            }
        }
    }

    private fun parseVmess(link: String, subscriptionUrl: String?): VpnProfileEntity? {
        return try {
            val rawB64 = link.substring(8).trim()
            val decoded = String(Base64.decode(rawB64, Base64.DEFAULT), Charsets.UTF_8)
            val json = JSONObject(decoded)
            
            val name = json.optString("ps", "VMess Node")
            val host = json.optString("add", "unknown")
            val port = json.optInt("port", 443)

            VpnProfileEntity(
                name = name,
                protocol = "VMESS",
                host = host,
                port = port,
                fullConfig = link,
                subscriptionUrl = subscriptionUrl
            )
        } catch (e: Exception) {
            parseUriCompatible(link, "VMESS", subscriptionUrl)
        }
    }

    private fun parseShadowsocks(link: String, subscriptionUrl: String?): VpnProfileEntity? {
        return try {
            val uri = URI(link)
            val host = uri.host ?: "unknown"
            val port = if (uri.port != -1) uri.port else 443
            var name = "Shadowsocks Node"
            if (link.contains("#")) {
                name = URLDecoder.decode(link.substringAfter("#"), "UTF-8")
            }
            
            VpnProfileEntity(
                name = name,
                protocol = "SS",
                host = host,
                port = port,
                fullConfig = link,
                subscriptionUrl = subscriptionUrl
            )
        } catch (e: Exception) {
            parseUriCompatible(link, "SS", subscriptionUrl)
        }
    }
}
