
package ro.pontaj.app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.AlertDialog
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
import android.util.Base64

class MainActivity : Activity() {

    private val prefs by lazy {
        getSharedPreferences("pontaj_data", Context.MODE_PRIVATE)
    }

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ro", "RO"))

    private var contracts = mutableListOf<String>()
    private var activeContract = "General"

    private var startDate = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    private var endDate = Calendar.getInstance()

    private var hourlyRate = 0.0
    private var currency = "RON"

    private lateinit var content: LinearLayout
    private lateinit var contractText: TextView
    private lateinit var periodText: TextView
    private lateinit var totalHours: TextView
    private lateinit var totalPay: TextView
    private lateinit var startButton: Button
    private lateinit var endButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contracts = prefs.getStringSet("contracts", setOf("General"))
            ?.toMutableList() ?: mutableListOf("General")

        if (contracts.isEmpty()) contracts.add("General")

        activeContract = prefs.getString("active_contract", contracts.first())
            ?: contracts.first()

        if (activeContract !in contracts) activeContract = contracts.first()

        loadContractSettings()
        loadContractPeriod()
        buildScreen()
        showPeriod()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun contractId(): String =
        Base64.encodeToString(
            activeContract.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

    private fun dateKey(date: Calendar): String =
        "%04d-%02d-%02d".format(
            date.get(Calendar.YEAR),
            date.get(Calendar.MONTH) + 1,
            date.get(Calendar.DAY_OF_MONTH)
        )

    private fun dataKey(date: Calendar, type: String): String =
        "${contractId()}_${dateKey(date)}_$type"

    private fun settingKey(type: String): String =
        "${contractId()}_$type"

    private fun loadContractSettings() {
        hourlyRate = prefs.getString(settingKey("rate"), "0")
            ?.toDoubleOrNull() ?: 0.0

        currency = prefs.getString(settingKey("currency"), "RON") ?: "RON"
    }

    private fun loadContractPeriod() {
        val defaultStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val defaultEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val savedStart = prefs.getLong(settingKey("period_start"), defaultStart.timeInMillis)
        val savedEnd = prefs.getLong(settingKey("period_end"), defaultEnd.timeInMillis)

        startDate = Calendar.getInstance().apply { timeInMillis = savedStart }
        endDate = Calendar.getInstance().apply { timeInMillis = savedEnd }
    }

    private fun saveContractPeriod(): Boolean =
        prefs.edit()
            .putLong(settingKey("period_start"), startDate.timeInMillis)
            .putLong(settingKey("period_end"), endDate.timeInMillis)
            .commit()

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val title = TextView(this).apply {
            text = "PONTAJ"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(15, 42, 78))
            gravity = Gravity.CENTER
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "by Misculae"
            textSize = 19f
            typeface = Typeface.create("serif", Typeface.ITALIC)
            setTextColor(Color.rgb(190, 140, 35))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(subtitle)

        val contractButton = Button(this).apply {
            text = "Contract: $activeContract"
            textSize = 15f
            setOnClickListener { manageContracts() }
        }
        root.addView(contractButton)
        contractText = contractButton

        periodText = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, dp(8), 0, dp(4))
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

        dateButtons.addView(
            startButton,
            LinearLayout.LayoutParams(0, dp(50), 1f)
        )
        dateButtons.addView(
            endButton,
            LinearLayout.LayoutParams(0, dp(50), 1f)
        )
        root.addView(dateButtons)

        root.addView(Button(this).apply {
            text = "Setare tarif orar"
            setOnClickListener { editRate() }
        })

        totalHours = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 70, 110))
            setPadding(dp(8), dp(8), dp(8), dp(4))
        }
        root.addView(totalHours)

        totalPay = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 120, 70))
            setPadding(dp(8), dp(4), dp(8), dp(10))
        }
        root.addView(totalPay)

        val saveButton = Button(this).apply {
            text = "SALVEAZĂ PONTAJUL"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val saved = saveContractPeriod() && prefs.edit().commit()
                Toast.makeText(
                    this@MainActivity,
                    if (saved) "Pontajul a fost salvat." else "Eroare la salvare. Încearcă din nou.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        root.addView(
            saveButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        contractText = root.getChildAt(2) as TextView
    }

    private fun manageContracts() {
        val options = mutableListOf(
            "Adaugă contract nou",
            "Șterge contract"
        )

        options.addAll(contracts.map { "Selectează: $it" })

        AlertDialog.Builder(this)
            .setTitle("Contracte salvate")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> addContract()
                    1 -> deleteContract()
                    else -> {
                        activeContract = contracts[which - 2]
                        prefs.edit()
                            .putString("active_contract", activeContract)
                            .apply()
                        loadContractSettings()
                        loadContractPeriod()
                        updateContractTitle()
                        showPeriod()
                    }
                }
            }
            .show()
    }

    private fun addContract() {
        val input = EditText(this).apply {
            hint = "Exemplu: Singapore"
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        AlertDialog.Builder(this)
            .setTitle("Contract nou")
            .setView(input)
            .setPositiveButton("Salvează") { _, _ ->
                val name = input.text.toString().trim()

                if (name.isNotEmpty() && name !in contracts) {
                    contracts.add(name)
                    prefs.edit()
                        .putStringSet("contracts", contracts.toSet())
                        .apply()

                    activeContract = name
                    prefs.edit()
                        .putString("active_contract", activeContract)
                        .apply()

                    loadContractSettings()
                    updateContractTitle()
                    showPeriod()
                } else {
                    Toast.makeText(
                        this,
                        "Numele este gol sau există deja.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun deleteContract() {
        if (contracts.size <= 1) {
            Toast.makeText(
                this,
                "Trebuie să păstrezi cel puțin un contract.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Șterge contract")
            .setItems(contracts.toTypedArray()) { _, which ->
                val removed = contracts[which]

                AlertDialog.Builder(this)
                    .setMessage("Ștergi contractul $removed?")
                    .setPositiveButton("Șterge") { _, _ ->
                        contracts.remove(removed)

                        if (activeContract == removed) {
                            activeContract = contracts.first()
                            prefs.edit()
                                .putString("active_contract", activeContract)
                                .apply()
                        }

                        prefs.edit()
                            .putStringSet("contracts", contracts.toSet())
                            .apply()

                        loadContractSettings()
                        updateContractTitle()
                        showPeriod()
                    }
                    .setNegativeButton("Anulează", null)
                    .show()
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun updateContractTitle() {
        contractText.text = "Contract: $activeContract"
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
                    if (startDate.after(endDate)) {
                        endDate = chosen.clone() as Calendar
                    }
                } else {
                    endDate = chosen
                    if (endDate.before(startDate)) {
                        startDate = chosen.clone() as Calendar
                    }
                }

                saveContractPeriod()
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
        val currencies = arrayOf("RON", "EUR", "USD")
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

        AlertDialog.Builder(this)
            .setTitle("Tarif pentru $activeContract")
            .setView(box)
            .setPositiveButton("Salvează") { _, _ ->
                hourlyRate = input.text.toString()
                    .replace(",", ".").toDoubleOrNull() ?: 0.0

                currency = spinner.selectedItem.toString()

                prefs.edit()
                    .putString(settingKey("rate"), hourlyRate.toString())
                    .putString(settingKey("currency"), currency)
                    .apply()

                showPeriod()
            }
            .setNegativeButton("Anulează", null)
            .show()
    }

    private fun getTime(date: Calendar, type: String): String =
        prefs.getString(dataKey(date, type), "") ?: ""

    private fun saveTime(date: Calendar, type: String, value: String) {
        prefs.edit().putString(dataKey(date, type), value).commit()
    }

    private fun enterHour(date: Calendar, type: String) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 22f
            gravity = Gravity.CENTER
            hint = "Exemplu: 8 sau 16"
            setText(getTime(date, type))
            selectAll()
        }

        val isEntry = type == "in"
        val title = if (isEntry) "Ora de intrare" else "Ora de ieșire"
        val positiveLabel = if (isEntry) "Continuă" else "Gata"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Introdu ora între 0 și 23, fără minute.")
            .setView(input)
            .setPositiveButton(positiveLabel) { _, _ ->
                val value = input.text.toString().trim()
                val hour = value.toIntOrNull()

                if (hour != null && hour in 0..23) {
                    // Salvare automată imediat după confirmarea orei.
                    saveTime(date, type, hour.toString())

                    if (isEntry) {
                        // După intrare, se deschide automat introducerea ieșirii.
                        showPeriod()
                        enterHour(date, "out")
                    } else {
                        showPeriod()
                        Toast.makeText(
                            this,
                            "Pontaj salvat automat.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Introdu o oră între 0 și 23.",
                        Toast.LENGTH_SHORT
                    ).show()
                    enterHour(date, type)
                }
            }
            .setNeutralButton("Șterge") { _, _ ->
                saveTime(date, type, "")
                showPeriod()
            }
            .setNegativeButton("Anulează", null)
            .show()
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
