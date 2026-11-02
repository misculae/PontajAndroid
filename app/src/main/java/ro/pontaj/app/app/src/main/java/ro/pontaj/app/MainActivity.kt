
package ro.pontaj.app

import android.app.Activity
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("pontaj_data", Context.MODE_PRIVATE)
    }

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ro", "RO"))

    private var startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    private var endDate = Calendar.getInstance()

    private var hourlyRate = 0.0
    private var currency = "RON"

    private lateinit var content: LinearLayout
    private lateinit var periodText: TextView
    private lateinit var totalHours: TextView
    private lateinit var totalPay: TextView
    private lateinit var startButton: Button
    private lateinit var endButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hourlyRate = prefs.getString("rate", "0")?.toDoubleOrNull() ?: 0.0
        currency = prefs.getString("currency", "RON") ?: "RON"

        buildScreen()
        showPeriod()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dateKey(date: Calendar): String =
        "%04d-%02d-%02d".format(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH) + 1,
            date.get(Calendar.DAY_OF_MONTH)
        )

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val title = TextView(this).apply {
            text = "PONTAJ BY MISCULAE"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(25, 45, 75))
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(14))
        }
        root.addView(title)

        periodText = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            text = "Selectează perioada"
        }
        root.addView(periodText)

        val dateButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        startButton = Button(this).apply {
            textSize = 12f
            setOnClickListener { chooseDate(true) }
        }

        endButton = Button(this).apply {
            textSize = 12f
            setOnClickListener { chooseDate(false) }
        }

        dateButtons.addView(startButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        dateButtons.addView(endButton, LinearLayout.LayoutParams(0, dp(50), 1f))
        root.addView(dateButtons)

        val settings = Button(this).apply {
            text = "Setare tarif orar"
            setOnClickListener { editRate() }
        }
        root.addView(settings)

        totalHours = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 70, 110))
            setPadding(dp(8), dp(8), dp(8), dp(4))
        }
        root.addView(totalHours)

        totalPay = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 120, 70))
            setPadding(dp(8), dp(4), dp(8), dp(10))
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

    private fun chooseDate(isStart: Boolean) {
        val selected = if (isStart) startDate else endDate

        DatePickerDialog(
            this,
            { _, y, m, d ->
                val chosen = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (isStart) {
                    startDate = chosen
                    if (startDate.after(endDate)) endDate = chosen.clone() as Calendar
                } else {
                    endDate = chosen
                    if (endDate.before(startDate)) startDate = chosen.clone() as Calendar
                }

                showPeriod()
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun editRate() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(hourlyRate.toString())
            hint = "Tarif pe oră"
        }

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("RON", "EUR", "USD")
        )
        spinner.setSelection(
            arrayOf("RON", "EUR", "USD").indexOf(currency).coerceAtLeast(0)
        )

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
                hourlyRate = input.text.toString()
                    .replace(",", ".").toDoubleOrNull() ?: 0.0
                currency = spinner.selectedItem.toString()

                prefs.edit()
                    .putString("rate", hourlyRate.toString())
                    .putString("currency", currency)
                    .apply()

                showPeriod()
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun key(date: Calendar, type: String): String =
        "${dateKey(date)}_$type"

    private fun getTime(date: Calendar, type: String): String =
        prefs.getString(key(date, type), "") ?: ""

    private fun saveTime(date: Calendar, type: String, value: String) {
        prefs.edit().putString(key(date, type), value).apply()
    }

    private fun workedHours(date: Calendar): Double {
        val start = getTime(date, "in")
        val end = getTime(date, "out")

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

    private fun chooseTime(date: Calendar, type: String) {
        val current = getTime(date, type)
        val now = Calendar.getInstance()

        var hour = now.get(Calendar.HOUR_OF_DAY)
        var minute = now.get(Calendar.MINUTE)

        if (current.contains(":")) {
            try {
                hour = current.substringBefore(":").toInt()
                minute = current.substringAfter(":").toInt()
            } catch (_: Exception) {}
        }

        TimePickerDialog(this, { _, h, m ->
            val value = "%02d:%02d".format(h, m)
            saveTime(date, type, value)
            showPeriod()
        }, hour, minute, true).show()
    }

    private fun showPeriod() {
        startButton.text = "De la: ${dateFormat.format(startDate.time)}"
        endButton.text = "Până la: ${dateFormat.format(endDate.time)}"

        periodText.text =
            "${dateFormat.format(startDate.time)} – ${dateFormat.format(endDate.time)}"

        content.removeAllViews()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(30, 55, 85))
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

        var sum = 0.0
        val current = startDate.clone() as Calendar

        while (!current.after(endDate)) {
            val date = current.clone() as Calendar
            val hours = workedHours(date)
            sum += hours

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(2), dp(5), dp(2), dp(5))

                setBackgroundColor(
                    when {
                        hours == 0.0 -> Color.rgb(235, 237, 240)
                        hours < 10 -> Color.WHITE
                        hours < 12 -> Color.rgb(220, 245, 220)
                        hours < 14 -> Color.rgb(160, 220, 165)
                        else -> Color.rgb(215, 125, 125)
                    }
                )
            }

            val dayText = TextView(this).apply {
                text = "%02d.%02d".format(
                    date.get(Calendar.DAY_OF_MONTH),
                    date.get(Calendar.MONTH) + 1
                )
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(dayText, LinearLayout.LayoutParams(0, dp(52), 0.9f))

            val entry = Button(this).apply {
                text = getTime(date, "in").ifBlank { "--:--" }
                textSize = 14f
                setOnClickListener { chooseTime(date, "in") }
            }
            row.addView(entry, LinearLayout.LayoutParams(0, dp(48), 1.1f))

            val exit = Button(this).apply {
                text = getTime(date, "out").ifBlank { "--:--" }
                textSize = 14f
                setOnClickListener { chooseTime(date, "out") }
            }
            row.addView(exit, LinearLayout.LayoutParams(0, dp(48), 1.1f))

            val hoursText = TextView(this).apply {
                text = if (hours == 0.0) "0" else "%.2f".format(hours)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
            }
            row.addView(hoursText, LinearLayout.LayoutParams(0, dp(52), 1f))

            content.addView(row)

            val divider = View(this).apply {
                setBackgroundColor(Color.rgb(210, 215, 220))
            }
            content.addView(divider, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            ))

            current.add(Calendar.DAY_OF_MONTH, 1)
        }

        totalHours.text = "Total ore: %.2f".format(sum)
        totalPay.text = "Total de plată: %.2f %s".format(
            sum * hourlyRate, currency
        )
    }
}
