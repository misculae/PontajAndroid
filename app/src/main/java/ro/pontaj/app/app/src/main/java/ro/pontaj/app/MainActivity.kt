
    
  
    }

    private fun workedHours(date: Calendar): Int {
        val start = getTime(date, "in").toIntOrNull() ?: return 0
        val end = getTime(date, "out").toIntOrNull() ?: return 0

        var result = end - start
        if (result < 0) result += 24
        return result
    }

    private fun showPeriod() {
        updateContractTitle()

        startButton.text = "De la: ${dateFormat.format(startDate.time)}"
        endButton.text = "Până la: ${dateFormat.format(endDate.time)}"

        periodText.text =
            "${dateFormat.format(startDate.time)} – ${dateFormat.format(endDate.time)}"

        content.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(15, 42, 78))
            setPadding(dp(3), dp(8), dp(3), dp(8))
        }

        listOf("Data", "Intrare", "Ieșire", "Ore").forEach {
            val text = TextView(this).apply {
                this.text = it
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            header.addView(text, LinearLayout.LayoutParams(0, dp(34), 1f))
        }

        content.addView(header)

        var sum = 0
        val current = startDate.clone() as Calendar

        while (!current.after(endDate)) {
            val date = current.clone() as Calendar
            val hours = workedHours(date)
            sum += hours

            val background = when {
                hours == 0 -> Color.rgb(235, 237, 240)
                hours < 10 -> Color.WHITE
                hours < 12 -> Color.rgb(220, 245, 220)
                hours < 14 -> Color.rgb(160, 220, 165)
                else -> Color.rgb(215, 125, 125)
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), dp(5), dp(2), dp(5))
                setBackgroundColor(background)
            }

            val dayText = TextView(this).apply {
                text = dateFormat.format(date.time)
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(dayText, LinearLayout.LayoutParams(0, dp(52), 1.1f))

            val entry = Button(this).apply {
                text = getTime(date, "in").ifBlank { "--" }
                textSize = 17f
                setOnClickListener { enterHour(date, "in") }
            }
            row.addView(entry, LinearLayout.LayoutParams(0, dp(48), 1f))

            val exit = Button(this).apply {
                text = getTime(date, "out").ifBlank { "--" }
                textSize = 17f
                setOnClickListener { enterHour(date, "out") }
            }
            row.addView(exit, LinearLayout.LayoutParams(0, dp(48), 1f))

            val hoursText = TextView(this).apply {
                text = hours.toString()
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(hoursText, LinearLayout.LayoutParams(0, dp(52), 0.8f))

            content.addView(row)

            val divider = View(this).apply {
                setBackgroundColor(Color.rgb(210, 215, 220))
            }
            content.addView(
                divider,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
            )

            current.add(Calendar.DAY_OF_MONTH, 1)
        }

        totalHours.text = "Total ore: $sum"
        totalPay.text = "Total de plată: %.2f %s".format(
            sum * hourlyRate,
            currency
        )
    }
}

