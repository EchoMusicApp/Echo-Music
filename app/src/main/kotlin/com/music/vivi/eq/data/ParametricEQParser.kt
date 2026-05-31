package iad1tya.echo.music.eq.data

import java.io.File


object ParametricEQParser {

    
    fun parseFile(file: File): ParametricEQ {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }

        return parseText(file.readText())
    }

    
    fun parseFile(filePath: String): ParametricEQ {
        return parseFile(File(filePath))
    }

    
    fun parseText(content: String): ParametricEQ {
        val lines = content.lines()
        var preamp = 0.0
        val bands = mutableListOf<ParametricEQBand>()
        val metadata = mutableMapOf<String, String>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            when {
                
                trimmedLine.startsWith("Preamp:", ignoreCase = true) -> {
                    preamp = parsePreamp(trimmedLine)
                }

                
                trimmedLine.startsWith("Filter", ignoreCase = true) -> {
                    val band = parseFilterLine(trimmedLine)
                    if (band != null) {
                        bands.add(band)
                    }
                }

                
                else -> {
                    val parts = trimmedLine.split(":", limit = 2)
                    if (parts.size == 2) {
                        metadata[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
        }

        return ParametricEQ(
            preamp = preamp,
            bands = bands,
            metadata = metadata
        )
    }

    
    private fun parsePreamp(line: String): Double {
        val regex = Regex("""Preamp:\s*([-+]?\d+\.?\d*)\s*dB""", RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    
    private fun parseFilterLine(line: String): ParametricEQBand? {
        try {
            
            if (!line.contains("ON", ignoreCase = true)) {
                return null
            }

            
            val filterType = parseFilterType(line) ?: return null

            
            val frequency = parseValue(line, "Fc", "Hz") ?: return null

            
            val gain = parseValue(line, "Gain", "dB") ?: return null

            
            val q = parseValue(line, "Q", null) ?: return null

            return ParametricEQBand(
                filterType = filterType,
                frequency = frequency,
                gain = gain,
                q = q
            )
        } catch (e: Exception) {
            println("Warning: Failed to parse filter line: $line")
            println("Error: ${e.message}")
            return null
        }
    }

    
    private fun parseFilterType(line: String): FilterType? {
        return when {
            line.contains("LSC", ignoreCase = true) -> FilterType.LSC
            line.contains("HSC", ignoreCase = true) -> FilterType.HSC
            line.contains("PK", ignoreCase = true) -> FilterType.PK
            line.contains("LPQ", ignoreCase = true) -> FilterType.LPQ
            line.contains("HPQ", ignoreCase = true) -> FilterType.HPQ
            else -> null
        }
    }

    
    private fun parseValue(line: String, keyword: String, unit: String?): Double? {
        val unitPattern = if (unit != null) "\\s*$unit" else ""
        val regex = Regex("""$keyword\s+([-+]?\d+\.?\d*)$unitPattern""", RegexOption.IGNORE_CASE)
        val match = regex.find(line)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    
    fun toString(eq: ParametricEQ): String {
        val sb = StringBuilder()
        sb.appendLine("Preamp: ${eq.preamp} dB")
        eq.bands.forEachIndexed { index, band ->
            sb.appendLine(
                "Filter ${index + 1}: ${band.filterType} Fc ${band.frequency} Hz " +
                        "Gain ${band.gain} dB Q ${band.q}"
            )
        }
        return sb.toString()
    }

    
    fun toFileFormat(eq: ParametricEQ): String {
        val sb = StringBuilder()
        sb.appendLine("Preamp: ${eq.preamp} dB")
        eq.bands.forEachIndexed { index, band ->
            sb.appendLine(
                "Filter ${index + 1}: ON ${band.filterType} " +
                        "Fc ${band.frequency.toInt()} Hz " +
                        "Gain ${band.gain} dB " +
                        "Q ${String.format("%.2f", band.q)}"
            )
        }
        return sb.toString()
    }

    
    fun validate(eq: ParametricEQ): List<String> {
        val errors = mutableListOf<String>()

        
        if (eq.preamp < -50.0 || eq.preamp > 50.0) {
            errors.add("Preamp value ${eq.preamp} dB is out of range (-50 to +50 dB)")
        }

        
        if (eq.bands.isEmpty()) {
            errors.add("EQ profile must have at least one band")
        }

        
        if (eq.bands.size > ParametricEQ.MAX_BANDS) {
            errors.add("EQ profile has ${eq.bands.size} bands, maximum is ${ParametricEQ.MAX_BANDS}")
        }

        
        eq.bands.forEachIndexed { index, band ->
            
            if (band.frequency <= 0.0 || band.frequency > 100000.0) {
                errors.add("Band ${index + 1}: Frequency ${band.frequency} Hz is out of range (1 to 100000 Hz)")
            }

            
            if (band.gain < -30.0 || band.gain > 30.0) {
                errors.add("Band ${index + 1}: Gain ${band.gain} dB is out of range (-30 to +30 dB)")
            }

            
            if (band.q <= 0.0 || band.q > 20.0) {
                errors.add("Band ${index + 1}: Q factor ${band.q} is out of range (0.01 to 20)")
            }
        }

        return errors
    }
}