
package ro.pontaj.app

import android.app.Activity
import android.app.TimePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import android.text.InputType
import android.content.Context
import java.util.Calendar

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("pontaj_data", Context.MODE_PRIVATE)
    }

    private var month = Calendar.getInstance().get(Calendar.MONTH)
    private var year = Calendar.getInstance().get(Calendar.YEAR)
    private var hourlyRate = 0.0
    private var currency = "RON"

    private lateinit var content: LinearLayout
    private lateinit var monthTitle: TextView
    private lateinit var totalHours: TextView
    private lateinit var totalPay: TextView

    private val months = arrayOf(
        "Ianuarie", "Februarie", "Martie", "Aprilie",
        "Mai", "Iunie", "Iulie", "August",
        "Septembrie", "Octombrie", "Noiembrie", "Decembrie"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hourlyRate = prefs.getString("rate", "0")?.toDoubleOrNull() ?: 0.0
        currency = prefs.getString("currency", "RON") ?: "RON"

        buildScreen()
        showMonth()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val title = TextView(this).apply {
            text = "PONTAJ BY MISCUALAE"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 45, 75))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(16))
        }
        root.addView(title)

        val navigation = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }

        val previous = Button(this).apply {
            text = "‹"
            setOnClickListener {
                month--
                if (month < 0) {
                    month = 11
                    year--
                }
                showMonth()
            }
        }

        monthTitle = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
        }

        val next = Button(this).apply {
            text = "›"
            setOnClickListener {
                month++
                if (month > 11) {
                    month = 0
                    year++
                }
                showMonth()
            }
        }

        navigation.addView(previous)
        navigation.addView(monthTitle, LinearLayout.LayoutParams(0, dp(48), 1f))
        navigation.addView(next)
        root.addView(navigation)

        val settings = Button(this).apply {
            text = "Setare tarif orar"
            setOnClickListener { editRate() }
        }
        root.addView(settings)

        totalHours = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 70, 110))
            setPadding(dp(8), dp(12), dp(8), dp(5))
        }
        root.addView(totalHours)

        totalPay = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 120, 70))
            setPadding(dp(8), dp(4), dp(8), dp(12))
        }
        root.addView(totalPay)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
    }

    private fun editRate() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(hourlyRate.toString())
            hint = "Tarif pe oră"
        }

        val currencies = arrayOf("RON", "EUR", "USD")
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            currencies
        )
        spinner.setSelection(currencies.indexOf(currency).coerceAtLeast(0))

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input)
            addView(spinner)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Tarif orar")
            .setView(box)
            .setPositiveButton("Salvează") { _, _ ->
                hourlyRate = input.text.toString().replace(",", ".")
                    .toDoubleOrNull() ?: 0.0
                currency = spinner.selectedItem.toString()
                prefs.edit()
                    .putString("rate", hourlyRate.toString())
                    .putString("currency", currency)
                    .apply()
                showMonth()
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun key(day: Int, type: String): String =
        "${year}_${month}_${day}_$type"

    private fun getTime(day: Int, type: String): String =
        prefs.getString(key(day, type), "") ?: ""

    private fun saveTime(day: Int, type: String, value: String) {
        prefs.edit().putString(key(day, type), value).apply()
    }

    private fun workedHours(day: Int): Double {
        val start = getTime(day, "in")
        val end = getTime(day, "out")
        if (start.isBlank() || end.isBlank()) return 0.0

        return try {
            val a = start.split(":")
            val b = end.split(":")
            val startMinutes = a[0].toInt() * 60 + a[1].toInt()
            var endMinutes = b[0].toInt() * 60 + b[1].toInt()
            if (endMinutes < startMinutes) endMinutes += 24 * 60
            (endMinutes - startMinutes) / 60.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun chooseTime(day: Int, type: String) {
        val calendar = Calendar.getInstance()
        val current = getTime(day, type)
        var hour = calendar.get(Calendar.HOUR_OF_DAY)
        var minute = calendar.get(Calendar.MINUTE)

        if (current.contains(":")) {
            try {
                hour = current.substringBefore(":").toInt()
                minute = current.substringAfter(":").toInt()
            } catch (_: Exception) {}
        }

        TimePickerDialog(this, { _, h, m ->
            val value = "%02d:%02d".format(h, m)
            saveTime(day, type, value)
            showMonth()
        }, hour, minute, true).show()
    }

    private fun showMonth() {
        monthTitle.text = "${months[month]} $year"
        content.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(30, 55, 85))
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }

        listOf("Zi", "Intrare", "Ieșire", "Ore").forEach {
            val text = TextView(this).apply {
                this.text = it
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            header.addView(text, LinearLayout.LayoutParams(0, dp(32), 1f))
        }
        content.addView(header)

        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        val days = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        var sum = 0.0

        for (day in 1..days) {
            val hours = workedHours(day)
            sum += hours

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(3), dp(5), dp(3), dp(5))
                setBackgroundColor(
                    when {
                        hours == 0.0 -> Color.rgb(235, 237, 240)
                        hours < 10 -> Color.WHITE
                        hours < 12 -> Color.rgb(220, 245, 220)
                        hours < 14 -> Color.rgb(160, 220, 165)
                        else -> Color.rgb(90, 175, 105)
                    }
                )
            }

            val dayText = TextView(this).apply {
                text = day.toString()
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(dayText, LinearLayout.LayoutParams(0, dp(48), 0.65f))

            val entry = Button(this).apply {
                text = getTime(day, "in").ifBlank { "--:--" }
                textSize = 11f
                setOnClickListener { chooseTime(day, "in") }
            }
            row.addView(entry, LinearLayout.LayoutParams(0, dp(44), 1.15f))

            val exit = Button(this).apply {
                text = getTime(day, "out").ifBlank { "--:--" }
                textSize = 11f
                setOnClickListener { chooseTime(day, "out") }
            }
            row.addView(exit, LinearLayout.LayoutParams(0, dp(44), 1.15f))

            val hoursText = TextView(this).apply {
                text = if (hours == 0.0) "0" else "%.2f".format(hours)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(hoursText, LinearLayout.LayoutParams(0, dp(48), 1f))

            content.addView(row)

            val divider = View(this).apply {
                setBackgroundColor(Color.rgb(210, 215, 220))
            }
            content.addView(divider, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ))
        }

        totalHours.text = "Total ore: %.2f".format(sum)
        totalPay.text = "Total de plată: %.2f %s".format(
            sum * hourlyRate, currency
        )
    }
}
