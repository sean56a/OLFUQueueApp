package com.example.olfuantipoloregistrarqueueingmanagementsystem

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.volley.*
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.InputStream

class FormFragment : Fragment() {

    private var attachmentUri: Uri? = null

    // Views
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var etStudentNumber: EditText
    private lateinit var etSection: EditText
    private lateinit var spLastSchoolYear: Spinner
    private lateinit var spLastSemester: Spinner
    private lateinit var spDepartment: Spinner
    private lateinit var documentContainer: LinearLayout
    private lateinit var etNotes: EditText
    private lateinit var btnUpload: Button
    private lateinit var btnSubmit: Button

    private val schoolYears = listOf("2020-2021","2021-2022","2022-2023","2023-2024","2024-2025")
    private val semesters = listOf("First Semester","Second Semester","Third Semester")
    private val departmentIds = mutableListOf<String>()
    private val checkBoxes = mutableListOf<CheckBox>()

    // File picker
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            attachmentUri = it
            btnUpload.text = getFileName(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_form, container, false)

        // Initialize views
        tvFirstName = view.findViewById(R.id.tvFirstName)
        tvLastName = view.findViewById(R.id.tvLastName)
        etStudentNumber = view.findViewById(R.id.etStudentNumber)
        etSection = view.findViewById(R.id.etSection)
        spLastSchoolYear = view.findViewById(R.id.spLastSchoolYear)
        spLastSemester = view.findViewById(R.id.spLastSemester)
        spDepartment = view.findViewById(R.id.spDepartment)
        documentContainer = view.findViewById(R.id.documentContainer)
        etNotes = view.findViewById(R.id.etNotes)
        btnUpload = view.findViewById(R.id.btnUpload)
        btnSubmit = view.findViewById(R.id.btnSubmit)

        // Load user data from bundle
        val bundle = arguments
        tvFirstName.text = bundle?.getString("first_name") ?: "First Name"
        tvLastName.text = bundle?.getString("last_name") ?: "Last Name"

        // Setup spinners
        setupSpinner(spLastSchoolYear, listOf("-- Last School Year Attended --") + schoolYears)
        setupSpinner(spLastSemester, listOf("-- Last Semester Attended --") + semesters)
        loadDepartments()
        loadDocuments()

        // Button listeners
        btnUpload.setOnClickListener { pickFileLauncher.launch("*/*") }
        btnSubmit.setOnClickListener { submitForm() }

        return view
    }

    // Spinner setup
    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val ctx = context ?: return
        spinner.adapter = object : ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_dropdown_item, items) {
            override fun isEnabled(position: Int) = position != 0
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tv = view as? TextView
                tv?.setTextColor(if (position == 0) 0xFF888888.toInt() else 0xFF000000.toInt())
                tv?.textSize = 16f
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Load departments from API
    private fun loadDepartments() {
        val ctx = context ?: return
        val url = "https://olfu-registrar.ellequin.com/api/get_departments.php"
        val request = com.android.volley.toolbox.StringRequest(Request.Method.GET, url,
            { response ->
                val json = JSONObject(response)
                if (json.getString("status") == "success") {
                    val deptArray = json.getJSONArray("departments")
                    val deptList = mutableListOf("-- Select Department --")
                    departmentIds.clear()
                    for (i in 0 until deptArray.length()) {
                        val obj = deptArray.getJSONObject(i)
                        deptList.add(obj.getString("name"))
                        departmentIds.add(obj.getString("id"))
                    }
                    setupSpinner(spDepartment, deptList)
                } else {
                    Toast.makeText(ctx, "Failed to load departments", Toast.LENGTH_SHORT).show()
                }
            },
            { _ -> Toast.makeText(ctx, "Error loading departments", Toast.LENGTH_SHORT).show() }
        )
        Volley.newRequestQueue(ctx).add(request)
    }

    // Load documents from API
    private fun loadDocuments() {
        val ctx = context ?: return
        val url = "https://olfu-registrar.ellequin.com/api/get_documents.php"
        val request = com.android.volley.toolbox.StringRequest(Request.Method.GET, url,
            { response ->
                val json = JSONObject(response)
                if (json.getString("status") == "success") {
                    val docArray = json.getJSONArray("documents")
                    documentContainer.removeAllViews()
                    checkBoxes.clear()
                    for (i in 0 until docArray.length()) {
                        val doc = docArray.getJSONObject(i)
                        val cb = CheckBox(ctx)
                        cb.text = doc.getString("name")
                        cb.tag = doc.getString("name")
                        cb.textSize = 16f
                        cb.setPadding(8, 8, 8, 8)
                        documentContainer.addView(cb)
                        checkBoxes.add(cb)
                    }
                } else {
                    Toast.makeText(ctx, "Failed to load documents", Toast.LENGTH_SHORT).show()
                }
            },
            { _ -> Toast.makeText(ctx, "Error loading documents", Toast.LENGTH_SHORT).show() }
        )
        Volley.newRequestQueue(ctx).add(request)
    }

    // Get file name from URI
    private fun getFileName(uri: Uri): String {
        val ctx = context ?: return "attachment"
        var name = "attachment"
        val cursor = ctx.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) name = it.getString(idx)
            }
        }
        return name
    }

    // Submit form
    private fun submitForm() {
        val ctx = context ?: return
        val firstName = tvFirstName.text.toString().trim()
        val lastName = tvLastName.text.toString().trim()
        val studentNumber = etStudentNumber.text.toString().trim()
        val section = etSection.text.toString().trim()
        val lastSchoolYear = if (spLastSchoolYear.selectedItemPosition == 0) "" else spLastSchoolYear.selectedItem.toString()
        val lastSemester = if (spLastSemester.selectedItemPosition == 0) "" else spLastSemester.selectedItem.toString()
        val departmentId = if (spDepartment.selectedItemPosition == 0) "" else departmentIds[spDepartment.selectedItemPosition - 1]
        val selectedDocs = checkBoxes.filter { it.isChecked }.map { it.tag.toString() }
        val documentsStr = selectedDocs.joinToString(", ")

        if (firstName.isEmpty() || lastName.isEmpty() || studentNumber.isEmpty() ||
            section.isEmpty() || lastSchoolYear.isEmpty() || lastSemester.isEmpty() ||
            departmentId.isEmpty() || documentsStr.isEmpty()
        ) {
            Toast.makeText(ctx, "Please fill all required fields.", Toast.LENGTH_LONG).show()
            return
        }

        val url = "https://olfu-registrar.ellequin.com/api/submit_request.php"

        val multipartRequest = object : VolleyMultipartRequest(
            Method.POST, url,
            Response.Listener { response ->
                val json = JSONObject(String(response.data))
                if (json.getString("status") == "success") {
                    Toast.makeText(ctx, "Request submitted successfully!", Toast.LENGTH_LONG).show()
                    clearForm()
                } else {
                    Toast.makeText(ctx, "Submission failed: ${json.getString("message")}", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(ctx, "Submission failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "first_name" to firstName,
                    "last_name" to lastName,
                    "student_number" to studentNumber,
                    "section" to section,
                    "department" to departmentId,
                    "last_school_year" to lastSchoolYear,
                    "last_semester" to lastSemester,
                    "document" to documentsStr,
                    "notes" to etNotes.text.toString().trim(),
                    "walk_in" to "0"
                )
            }

            override fun getByteData(): MutableMap<String, DataPart>? {
                attachmentUri?.let {
                    val inputStream: InputStream? = ctx.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        return hashMapOf("attachment" to DataPart(getFileName(it), bytes))
                    }
                }
                return null
            }
        }

        Volley.newRequestQueue(ctx).add(multipartRequest)
    }

    private fun clearForm() {
        etStudentNumber.text.clear()
        etSection.text.clear()
        etNotes.text.clear()
        btnUpload.text = "Upload Attachments (Images/PDFs)"
        attachmentUri = null
        spLastSchoolYear.setSelection(0)
        spLastSemester.setSelection(0)
        spDepartment.setSelection(0)
        checkBoxes.forEach { it.isChecked = false }
    }
}
