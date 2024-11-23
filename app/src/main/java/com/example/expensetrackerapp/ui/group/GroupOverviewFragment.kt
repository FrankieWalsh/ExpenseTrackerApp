package com.example.expensetrackerapp.ui.group

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Expense
import com.example.expensetrackerapp.model.ExpenseSplit
import com.example.expensetrackerapp.model.User
import com.example.expensetrackerapp.ui.expense.ExpenseAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupOverviewFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var groupId: String? = null
    private val allExpenses = mutableListOf<Expense>()
    private val filteredExpenses = mutableListOf<Expense>()
    private lateinit var expenseAdapter: ExpenseAdapter
    private val selectedCategories = mutableSetOf<String>()
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val groupMembers = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_group_overview, container, false)
        firestore = FirebaseFirestore.getInstance()
        groupId = arguments?.getString("groupId")

        // Initialize UI elements
        val groupNameTextView = view.findViewById<TextView>(R.id.textViewGroupName)
        val groupDescriptionTextView = view.findViewById<TextView>(R.id.textViewGroupDescription)
        val editButton = view.findViewById<Button>(R.id.buttonEditGroup)
        val expensesRecyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewExpenses)
        val addExpenseButton = view.findViewById<ImageView>(R.id.fabAddExpense)
        val linearLayoutGroupMembers = view.findViewById<LinearLayout>(R.id.linearLayoutGroupMembers)

        val autoCompleteFilterCategories: AutoCompleteTextView =
            view.findViewById(R.id.autoCompleteFilterCategories)

        val categories = resources.getStringArray(R.array.expense_categories)
        updateDialogText(autoCompleteFilterCategories)
        categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_multiple_choice, categories
        )
        autoCompleteFilterCategories.setAdapter(categoryAdapter)

        autoCompleteFilterCategories.setOnClickListener {
            showMultiSelectDialog(categories, autoCompleteFilterCategories)
        }

        // Set up Floating Action Button to create new expenses
        addExpenseButton.setOnClickListener {
            showAddExpenseDialog()
        }

        // Initialize RecyclerView for expenses
        expenseAdapter = ExpenseAdapter(filteredExpenses)


        expensesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        expensesRecyclerView.adapter = expenseAdapter

        // Fetch and display group data
        groupId?.let { id ->
            firestore.collection("groups").document(id).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val groupName = document.getString("name")
                        val groupDescription = document.getString("description")
                        val createdBy = document.getString("createdBy")

                        groupNameTextView.text = groupName ?: "Unnamed Group"
                        groupDescriptionTextView.text = groupDescription ?: "No Description"

                        // Show edit button only if current user is creator
                        if (createdBy == FirebaseAuth.getInstance().currentUser?.uid) {
                            editButton.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Edit button action
        editButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("groupId", groupId)
                putString("groupName", groupNameTextView.text.toString())
                putString("groupDescription", groupDescriptionTextView.text.toString())
            }
            findNavController().navigate(R.id.action_groupTabsFragment_to_groupDetailFragment, bundle)
        }

        // Fetch expenses for the group
        fetchExpenses()

        // Fetch and display group members
        fetchGroupMembers(linearLayoutGroupMembers)

        return view
    }

    private fun fetchGroupMembers(linearLayoutGroupMembers: LinearLayout) {
        groupId?.let { id ->
            firestore.collection("group_members")
                .whereEqualTo("groupId", id)
                .get()
                .addOnSuccessListener { result ->
                    val userIds = result.documents.mapNotNull { it.getString("userId") }
                    if (userIds.isNotEmpty()) {
                        // Fetch user details
                        firestore.collection("users")
                            .whereIn("id", userIds)
                            .get()
                            .addOnSuccessListener { userResult ->
                                // Clear existing views
                                linearLayoutGroupMembers.removeAllViews()
                                groupMembers.clear()
                                for (userDoc in userResult) {
                                    val user = userDoc.toObject(User::class.java)
                                    user.id = userDoc.id
                                    groupMembers.add(user)
                                    val displayName = user.name.takeIf { !it.isNullOrBlank() } ?: user.email

                                    // Create TextView for each member
                                    val memberTextView = TextView(requireContext()).apply {
                                        text = displayName
                                        setPadding(16, 8, 16, 8)
                                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                        setTypeface(null, Typeface.BOLD)
                                        background = ContextCompat.getDrawable(requireContext(), R.drawable.member_background)
                                    }

                                    // Add margin to the TextView
                                    val layoutParams = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        rightMargin = 8  // Adjust spacing between items
                                    }
                                    linearLayoutGroupMembers.addView(memberTextView, layoutParams)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching group members: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchExpenses() {
        groupId?.let { id ->
            firestore.collection("expenses")
                .whereEqualTo("groupId", id)
                .get()
                .addOnSuccessListener { result ->
                    allExpenses.clear()
                    for (document in result) {
                        val expense = document.toObject(Expense::class.java)
                        expense.id = document.id
                        allExpenses.add(expense)
                    }
                    updateFilteredExpenses()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateFilteredExpenses() {
        filteredExpenses.clear()
        filteredExpenses.addAll(
            if (selectedCategories.isEmpty()) allExpenses
            else allExpenses.filter { it.category in selectedCategories }
        )
        expenseAdapter.notifyDataSetChanged()
    }

    private fun showMultiSelectDialog(categories: Array<String>, autoCompleteTextView: AutoCompleteTextView) {
        val selectedItems = BooleanArray(categories.size) { selectedCategories.contains(categories[it]) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Categories")
            .setMultiChoiceItems(categories, selectedItems) { _, index, isChecked ->
                if (isChecked) {
                    selectedCategories.add(categories[index])
                } else {
                    selectedCategories.remove(categories[index])
                }
            }
            .setPositiveButton("OK") { _, _ ->
                updateDialogText(autoCompleteTextView)
                updateFilteredExpenses()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDialogText(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setText(
            if (selectedCategories.isEmpty()) "All Categories" else selectedCategories.joinToString(", ")
        )
    }

    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.editTextExpenseDescription)
        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextExpenseAmount)
        val spinnerCategory: Spinner = dialogView.findViewById(R.id.spinnerCategory)
        val listViewMembers = dialogView.findViewById<ListView>(R.id.listViewMembers)

        var selectedCategory = "General"
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Fetch group members for selection
        val memberNames = groupMembers.map { it.name.takeIf { name -> name.isNotBlank() } ?: it.email }
        val memberIds = groupMembers.map { it.id }

        val arrayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_multiple_choice, memberNames)
        listViewMembers.adapter = arrayAdapter
        listViewMembers.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val description = descriptionEditText.text.toString()
                val amount = amountEditText.text.toString().toDoubleOrNull()

                // Get selected member IDs
                val selectedPositions = listViewMembers.checkedItemPositions
                val selectedMemberIds = mutableListOf<String>()
                for (i in 0 until selectedPositions.size()) {
                    val position = selectedPositions.keyAt(i)
                    if (selectedPositions.valueAt(i)) {
                        selectedMemberIds.add(memberIds[position])
                    }
                }

                if (description.isEmpty() || amount == null || selectedMemberIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all fields and select at least one member", Toast.LENGTH_SHORT).show()
                } else {
                    addExpense(description, amount, FirebaseAuth.getInstance().currentUser?.uid ?: "", selectedCategory, selectedMemberIds)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addExpense(description: String, amount: Double, payerId: String, category: String, selectedMemberIds: List<String>) {
        val expense = Expense(
            groupId = groupId ?: "",
            amount = amount,
            description = description,
            payerId = payerId,
            createdAt = System.currentTimeMillis(),
            category = category
        )

        firestore.collection("expenses")
            .add(expense)
            .addOnSuccessListener { documentRef ->
                expense.id = documentRef.id
                allExpenses.add(0, expense)
                if (selectedCategories.isEmpty() || category in selectedCategories) {
                    filteredExpenses.add(0, expense)
                    expenseAdapter.notifyItemInserted(0)
                }
                addExpenseSplits(expense, selectedMemberIds)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addExpenseSplits(expense: Expense, selectedMemberIds: List<String>) {
        val memberCount = selectedMemberIds.size
        if (memberCount > 0) {
            val splitAmount = expense.amount / memberCount
            for (userId in selectedMemberIds) {
                // Skip creating a split for the payer
                if (userId == expense.payerId) continue

                val expenseSplit = ExpenseSplit(
                    expenseId = expense.id,
                    userId = userId,
                    groupId = groupId ?: "",
                    owedTo = expense.payerId,
                    amount = splitAmount,
                    hasPaid = false
                )

                firestore.collection("expense_splits").add(expenseSplit)
            }
        }
    }

    private fun showPaymentDialog(expense: Expense) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("expense_splits")
            .whereEqualTo("expenseId", expense.id)
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                val splitDocument = result.documents.firstOrNull()
                val split = splitDocument?.toObject(ExpenseSplit::class.java)
                val splitId = splitDocument?.id

                if (split != null && !split.hasPaid && splitId != null) {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pay_expense, null)
                    val splitAmountTextView = dialogView.findViewById<TextView>(R.id.textViewSplitAmount)
                    splitAmountTextView.text = "Your split: $${split.amount}"

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Pay Your Split")
                        .setView(dialogView)
                        .setPositiveButton("Pay") { _, _ ->
                            markSplitAsPaid(splitId)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching split: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markSplitAsPaid(splitId: String) {
        firestore.collection("expense_splits").document(splitId)
            .update("hasPaid", true)
            .addOnSuccessListener {
                fetchExpenses()
                Toast.makeText(requireContext(), "Payment marked as complete", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error marking payment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
