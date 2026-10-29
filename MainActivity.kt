package uz.eslatma

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDate

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var langBtn: Button
    private fun sp() = getSharedPreferences("eslatma", MODE_PRIVATE)
    private fun lang() = sp().getString("lang", "uz-UZ") ?: "uz-UZ"
    private fun langText() = if (lang() == "uz-UZ") "🇺🇿 O'zbekcha  (bosing: English)" else "🇬🇧 English  (tap: O'zbekcha)"
    private lateinit var list: LinearLayout

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun today() = LocalDate.now().toString()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
        langBtn = Button(this).apply {
            text = langText(); textSize = 18f
            setOnClickListener {
                sp().edit().putString("lang", if (lang() == "uz-UZ") "en-US" else "uz-UZ").apply()
                text = langText()
            }
        }
        root.addView(langBtn)
        root.addView(Button(this).apply {
            text = "🎤  GAPIRING"
            textSize = 30f
            setPadding(0, dp(36), 0, dp(36))
            setOnClickListener { speak() }
        })
        input = EditText(this).apply {
            hint = "yoki shu yerga yozing"
            textSize = 20f
            minLines = 2
            gravity = Gravity.TOP
        }
        root.addView(input)
        root.addView(Button(this).apply {
            text = "➕ Qo'shish"; textSize = 20f
            setOnClickListener { add(input.text.toString()); input.setText("") }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
        if (b == null) speak()
    }

    private fun speak() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang())
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang())
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Gapiring...")
        try { startActivityForResult(i, 7) }
        catch (e: Exception) { Toast.makeText(this, "Google ovoz xizmati topilmadi", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("simple")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 7 && res == RESULT_OK) {
            val t = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
            add(t)
        }
    }

    private fun add(t: String) {
        val ps = Parser.parse(t)
        if (ps.isEmpty()) return
        val all = Store.all(this)
        var id = System.currentTimeMillis() * 100
        ps.forEach { all.add(Item(id++, today(), -1, it, false)) }
        Store.save(this, all)
        Store.setStage(this, today(), 0)
        Notifier.post(this, today())
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        val items = Store.forDay(this, today())
        items.forEachIndexed { n, item ->
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            r.addView(TextView(this).apply {
                text = (if (item.done) "✅ " else "") + "${n + 1}. ${item.text}"
                textSize = 22f
                setPadding(0, dp(10), 0, dp(10))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            r.addView(Button(this).apply {
                text = "🗑"
                setOnClickListener {
                    val l = Store.all(this@MainActivity); l.removeAll { x -> x.id == item.id }
                    Store.save(this@MainActivity, l)
                    Notifier.post(this@MainActivity, item.day)
                    refresh()
                }
            })
            list.addView(r)
        }
    }

    override fun onResume() { super.onResume(); refresh() }
}
