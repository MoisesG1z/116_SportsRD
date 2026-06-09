package com.sports.redaccion.data

data class RssItem(
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String,
    val imageUrl: String?
)

object RssParser {
    
    fun parse(xml: String): List<RssItem> {
        val items = mutableListOf<RssItem>()
        var searchIndex = 0
        
        while (true) {
            val itemStart = xml.indexOf("<item>", searchIndex)
            if (itemStart == -1) break
            
            val itemEnd = xml.indexOf("</item>", itemStart)
            if (itemEnd == -1) break
            
            val itemXml = xml.substring(itemStart + 6, itemEnd)
            val parsedItem = parseItem(itemXml)
            if (parsedItem != null) {
                items.add(parsedItem)
            }
            
            searchIndex = itemEnd + 7
        }
        
        return items
    }
    
    private fun parseItem(itemXml: String): RssItem? {
        val title = extractTag(itemXml, "title") ?: return null
        val link = extractTag(itemXml, "link") ?: return null
        val rawDescription = extractTag(itemXml, "description") ?: ""
        val description = cleanHtml(rawDescription)
        val pubDate = extractTag(itemXml, "pubDate") ?: extractTag(itemXml, "pubdate") ?: ""
        
        // Search image in enclosure or media:content or description HTML
        val imageUrl = extractEnclosureUrl(itemXml) 
            ?: extractMediaContentUrl(itemXml)
            ?: extractImgSrcFromHtml(rawDescription)
            
        return RssItem(
            title = cleanCdata(title).trim(),
            link = cleanCdata(link).trim(),
            description = cleanCdata(description).trim(),
            pubDate = cleanCdata(pubDate).trim(),
            imageUrl = imageUrl?.let { cleanCdata(it).trim() }
        )
    }
    
    private fun extractTag(xml: String, tag: String): String? {
        val openTag = "<$tag"
        val closeTag = "</$tag>"
        
        val startIdx = xml.indexOf(openTag)
        if (startIdx == -1) return null
        
        // Find closing bracket of the opening tag, e.g. <title> or <title attr="...">
        val closeBracketIdx = xml.indexOf(">", startIdx)
        if (closeBracketIdx == -1) return null
        
        val endIdx = xml.indexOf(closeTag, closeBracketIdx)
        if (endIdx == -1) return null
        
        return xml.substring(closeBracketIdx + 1, endIdx)
    }
    
    private fun extractEnclosureUrl(xml: String): String? {
        // e.g. <enclosure url="https://..." type="image/jpeg" />
        val tagStart = xml.indexOf("<enclosure")
        if (tagStart == -1) return null
        val tagEnd = xml.indexOf(">", tagStart)
        if (tagEnd == -1) return null
        val tagContent = xml.substring(tagStart, tagEnd)
        return extractAttribute(tagContent, "url")
    }
    
    private fun extractMediaContentUrl(xml: String): String? {
        // e.g. <media:content url="https://..." ... />
        val tagStart = xml.indexOf("<media:content")
        if (tagStart == -1) return null
        val tagEnd = xml.indexOf(">", tagStart)
        if (tagEnd == -1) return null
        val tagContent = xml.substring(tagStart, tagEnd)
        return extractAttribute(tagContent, "url")
    }
    
    private fun extractImgSrcFromHtml(html: String): String? {
        val tagStart = html.indexOf("<img")
        if (tagStart == -1) return null
        val tagEnd = html.indexOf(">", tagStart)
        if (tagEnd == -1) return null
        val tagContent = html.substring(tagStart, tagEnd)
        return extractAttribute(tagContent, "src")
    }
    
    private fun extractAttribute(tagContent: String, attrName: String): String? {
        val attrPattern = "$attrName=\""
        val startIdx = tagContent.indexOf(attrPattern)
        if (startIdx == -1) {
            // Check single quotes
            val singleQuotePattern = "$attrName='"
            val startIdxSingle = tagContent.indexOf(singleQuotePattern)
            if (startIdxSingle == -1) return null
            val endIdxSingle = tagContent.indexOf("'", startIdxSingle + singleQuotePattern.length)
            if (endIdxSingle == -1) return null
            return tagContent.substring(startIdxSingle + singleQuotePattern.length, endIdxSingle)
        }
        val endIdx = tagContent.indexOf("\"", startIdx + attrPattern.length)
        if (endIdx == -1) return null
        return tagContent.substring(startIdx + attrPattern.length, endIdx)
    }
    
    private fun cleanCdata(text: String): String {
        var clean = text
        if (clean.contains("<![CDATA[")) {
            clean = clean.replace("<![CDATA[", "").replace("]]>", "")
        }
        return clean.trim()
    }
    
    private fun cleanHtml(html: String): String {
        // Simple HTML tags stripping
        var result = cleanCdata(html)
        var insideTag = false
        val sb = StringBuilder()
        
        var i = 0
        while (i < result.length) {
            val char = result[i]
            if (char == '<') {
                insideTag = true
            } else if (char == '>') {
                insideTag = false
            } else if (!insideTag) {
                sb.append(char)
            }
            i++
        }
        
        // Unescape standard HTML entities
        var finalStr = sb.toString()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace("&middot;", "·")
            
        // Limit length for description
        if (finalStr.length > 280) {
            finalStr = finalStr.substring(0, 277) + "..."
        }
        return finalStr.trim()
    }
}
