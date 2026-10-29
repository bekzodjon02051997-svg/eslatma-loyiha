package uz.eslatma

object Parser {
    fun parse(input: String): List<String> {
        val s = input.replace(Regex("[’‘ʻʼ`´]"), "'")
        return s.split(Regex("(?i)\\.(?!\\d)|[;!?\\n,]+|\\b(?:so'ngra|undan keyin|va keyin|keyingisi|keyingi|keyin|yana|and then|after that|then|next|also)\\b"))
            .map { it.trim().trim('.', ',', '-', '–') }
            .map { it.replace(Regex("(?i)^(va|ham|and)\\s+"), "") }
            .filter { it.length > 1 }
            .map { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
