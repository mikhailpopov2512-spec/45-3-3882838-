package com.example.data.util

import android.net.Uri
import android.util.Base64
import com.example.data.local.VpnProfileEntity
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object VpnLinkParser {

    fun parseSubscription(base64Content: String, subUrl: String): List<VpnProfileEntity> {
        val decoded = try {
            val clean = base64Content.replace("\n", "").replace("\r", "").trim()
            val data = Base64.decode(clean, Base64.DEFAULT)
            String(data, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // Try reading as plain-text list of URIs if it is not valid base64
            base64Content
        }

        val lines = decoded.split(Regex("[\\n\\r]+"))
        val profiles = mutableListOf<VpnProfileEntity>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val parsed = parseVpnLink(trimmed)
                if (parsed != null) {
                    profiles.add(parsed.copy(isSubProfile = true, subUrl = subUrl))
                }
            }
        }
        return profiles
    }

    fun parseVpnLink(link: String): VpnProfileEntity? {
        val trimmed = link.trim()
        return when {
            trimmed.startsWith("vless://", ignoreCase = true) -> parseVless(trimmed)
            trimmed.startsWith("vmess://", ignoreCase = true) -> parseVmess(trimmed)
            trimmed.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(trimmed)
            trimmed.startsWith("trojan://", ignoreCase = true) -> parseTrojan(trimmed)
            else -> null
        }
    }

    private fun parseVless(link: String): VpnProfileEntity? {
        // vless://uuid@host:port?query#remarks
        try {
            val uri = Uri.parse(link)
            val userInfo = uri.userInfo ?: return null
            val uuid = userInfo
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val remarks = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "VLESS Node"
            
            val security = uri.getQueryParameter("security") ?: "tls"
            val sni = uri.getQueryParameter("sni") ?: ""
            val path = uri.getQueryParameter("path") ?: ""
            val flow = uri.getQueryParameter("flow") ?: ""

            return VpnProfileEntity(
                name = remarks,
                protocol = "VLESS",
                host = host,
                port = port,
                uuid = uuid,
                security = security,
                sni = sni,
                path = path,
                flow = flow,
                remarks = remarks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseVmess(link: String): VpnProfileEntity? {
        // vmess://base64json
        try {
            val rawBase64 = link.substring(8).trim()
            val decodedBytes = Base64.decode(rawBase64, Base64.DEFAULT)
            val jsonString = String(decodedBytes, StandardCharsets.UTF_8)
            val json = JSONObject(jsonString)

            val v = json.optString("v", "2")
            val ps = json.optString("ps", "VMess Node")
            val add = json.optString("add", "")
            val portObj = json.opt("port")
            val port = when (portObj) {
                is Number -> portObj.toInt()
                is String -> portObj.toIntOrNull() ?: 443
                else -> 443
            }
            val id = json.optString("id", "")
            val aid = json.optInt("aid", 0)
            val net = json.optString("net", "tcp")
            val type = json.optString("type", "none")
            val host = json.optString("host", "")
            val path = json.optString("path", "")
            val tls = json.optString("tls", "none")
            val sni = json.optString("sni", "")

            if (add.isEmpty() || id.isEmpty()) return null

            return VpnProfileEntity(
                name = ps,
                protocol = "VMess",
                host = add,
                port = port,
                uuid = id,
                security = if (tls == "tls") "tls" else "none",
                sni = sni,
                path = path,
                remarks = "V=$v, AID=$aid, NET=$net, TYPE=$type, HOST=$host"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseShadowsocks(link: String): VpnProfileEntity? {
        // ss://base64(method:password)@host:port#remarks
        try {
            val uri = Uri.parse(link)
            val remarks = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Shadowsocks Node"
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 8388

            var userInfo = uri.userInfo ?: ""
            var methodAndPassword = ""
            if (userInfo.isNotEmpty()) {
                // UserInfo can be legacy base64 encoded method:password
                methodAndPassword = try {
                    val decoded = Base64.decode(userInfo, Base64.DEFAULT)
                    String(decoded, StandardCharsets.UTF_8)
                } catch (e: Exception) {
                    userInfo
                }
            } else {
                // Modern sip002 format might put Base64 in host part or path
                val authority = uri.encodedAuthority ?: ""
                val atIndex = authority.indexOf('@')
                if (atIndex != -1) {
                    val encodedUserInfo = authority.substring(0, atIndex)
                    methodAndPassword = try {
                        val decoded = Base64.decode(encodedUserInfo, Base64.DEFAULT)
                        String(decoded, StandardCharsets.UTF_8)
                    } catch (e: Exception) {
                        encodedUserInfo
                    }
                }
            }

            return VpnProfileEntity(
                name = remarks,
                protocol = "Shadowsocks",
                host = host,
                port = port,
                uuid = methodAndPassword, // Stored as uuid field
                security = "none",
                remarks = remarks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun parseTrojan(link: String): VpnProfileEntity? {
        // trojan://password@host:port?peer=sni#remarks
        try {
            val uri = Uri.parse(link)
            val password = uri.userInfo ?: return null
            val host = uri.host ?: return null
            val port = if (uri.port != -1) uri.port else 443
            val remarks = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Trojan Node"
            val sni = uri.getQueryParameter("peer") ?: uri.getQueryParameter("sni") ?: ""

            return VpnProfileEntity(
                name = remarks,
                protocol = "Trojan",
                host = host,
                port = port,
                uuid = password, // Stored key/pass as uuid
                security = "tls",
                sni = sni,
                remarks = remarks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun toVlessLink(profile: VpnProfileEntity): String {
        val encryptedRemarks = Uri.encode(profile.name)
        val query = mutableListOf<String>()
        if (profile.security.isNotEmpty()) query.add("security=${profile.security}")
        if (profile.sni.isNotEmpty()) query.add("sni=${profile.sni}")
        if (profile.path.isNotEmpty()) query.add("path=${Uri.encode(profile.path)}")
        if (profile.flow.isNotEmpty()) query.add("flow=${profile.flow}")
        
        val queryString = if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
        return "vless://${profile.uuid}@${profile.host}:${profile.port}$queryString#$encryptedRemarks"
    }

    fun toVmessLink(profile: VpnProfileEntity): String {
        val obj = JSONObject()
        obj.put("v", "2")
        obj.put("ps", profile.name)
        obj.put("add", profile.host)
        obj.put("port", profile.port)
        obj.put("id", profile.uuid)
        obj.put("aid", 0)
        obj.put("net", "tcp")
        obj.put("type", "none")
        obj.put("host", "")
        obj.put("path", profile.path)
        obj.put("tls", if (profile.security == "tls") "tls" else "none")
        obj.put("sni", profile.sni)
        
        val base64 = Base64.encodeToString(obj.toString().toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "vmess://$base64"
    }

    fun toShadowsocksLink(profile: VpnProfileEntity): String {
        val encodedUserInfo = Base64.encodeToString(profile.uuid.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        val encryptedRemarks = Uri.encode(profile.name)
        return "ss://$encodedUserInfo@${profile.host}:${profile.port}#$encryptedRemarks"
    }

    fun toTrojanLink(profile: VpnProfileEntity): String {
        val encryptedRemarks = Uri.encode(profile.name)
        val query = if (profile.sni.isNotEmpty()) "?peer=${profile.sni}" else ""
        return "trojan://${profile.uuid}@${profile.host}:${profile.port}$query#$encryptedRemarks"
    }
}
