package com.example.expensetrackerapp.ui.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.expensetrackerapp.R
import com.example.expensetrackerapp.model.Expense
import com.example.expensetrackerapp.model.ExpenseSplit
import com.example.expensetrackerapp.ui.expense.ExpenseAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupOverviewFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var groupId: String? = null
    private val expenses = mutableListOf<Expense>()
    private lateinit var expenseAdapter: ExpenseAdapter
    private val selectedCategories = mutableSetOf<String>() // Store selected categories
    private lateinit var categoryAdapter: ArrayAdapter<String>

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
        val addExpenseButton = view.findViewById<Button>(R.id.buttonAddExpense)

        val autoCompleteFilterCategories: AutoCompleteTextView =
            view.findViewById(R.id.autoCompleteFilterCategories)

        val categories = resources.getStringArray(R.array.expense_categories)
        updateDialogText(autoCompleteFilterCategories)
        categoryAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_multiple_choice, categories)
            autoCompleteFilterCategories.setAdapter(categoryAdapter)

        autoCompleteFilterCategories.setOnClickListener {
            showMultiSelectDialog(categories, autoCompleteFilterCategories)
        }


        // Setup Add Expense button
        addExpenseButton.setOnClickListener { showAddExpenseDialog() }

        // Initialize RecyclerView
        expenseAdapter = ExpenseAdapter(expenses) { expense ->
            showPaymentDialog(expense)
        }
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

        return view
    }

    private fun fetchExpenses() {
        groupId?.let { id ->
            firestore.collection("expenses")
                .whereEqualTo("groupId", id)
                .get()
                .addOnSuccessListener { result ->
                    expenses.clear()
                    for (document in result) {
                        val expense = document.toObject(Expense::class.java)
                        expense.id = document.id
                        expenses.add(expense)
                    }
                    expenseAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching expenses: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMultiSelectDialog(categories: Array<String>, autoCompleteTextView: AutoCompleteTextView) {
        val selectedItems = BooleanArray(categories.size) { selectedCategories.contains(categories[it]) }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Categories")
            .setMultiChoiceItems(categories, selectedItems) { _, index, isChecked ->
                if (isChecked) {
                    selectedCategories.add(categories[index]) // Add selected category
                } else {
                    selectedCategories.remove(categories[index]) // Remove unselected category
                }
            }
            .setPositiveButton("OK") { _, _ ->
                updateDialogText(autoCompleteTextView)
                filterExpensesByCategories(selectedCategories)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDialogText(autoCompleteTextView: AutoCompleteTextView) {
        autoCompleteTextView.setText(
            if (selectedCategories.isEmpty()) "All Categories" else selectedCategories.joinToString(", ")
        )
    }

    private fun filterExpensesByCategories(categories: Set<String>) {
        val allExpenses = expenses // Replace with your actual expense list
        val filteredExpenses = if (categories.isEmpty()) {
            allExpenses
        } else {
            allExpenses.filter { it.category in categories }
        }
        expenseAdapter.updateExpenses(filteredExpenses)

        // Update your RecyclerView adapter here
    }


    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_expense, null)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.editTextExpenseDescription)
        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextExpenseAmount)

        var selectedCategory = "General" // Default to "General"
        val spinnerCategory: Spinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected category
                selectedCategory = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: Handle the case where nothing is selected
            }
        }


        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val description = descriptionEditText.text.toString()
                val amount = amountEditText.text.toString().toDoubleOrNull()

                if (description.isEmpty() || amount == null) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                } else {
                    addExpense(description, amount, FirebaseAuth.getInstance().currentUser?.uid ?: "", selectedCategory)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addExpense(description: String, amount: Double, payerId: String, selectedCategory: String) {
        val expense = Expense(
            groupId = groupId ?: "",
            amount = amount,
            description = description,
            payerId = payerId,
            createdAt = System.currentTimeMillis(),
            category = selectedCategory
        )
        firestore.collection("expenses")
            .add(expense)
            .addOnSuccessListener { documentRef ->
                expense.id = documentRef.id
                expenses.add(0, expense)
                expenseAdapter.notifyItemInserted(0)
                addExpenseSplits(expense)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // GroupOverviewFragment.kt
    private fun addExpenseSplits(expense: Expense) {
        groupId?.let { groupId ->
            firestore.collection("group_members")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener { result ->
                    val memberCount = result.size()
                    if (memberCount > 0) {
                        val splitAmount = expense.amount / memberCount
                        for (document in result) {
                            val userId = document.getString("userId") ?: continue

                            // Skip creating a split for the person who created the expense
                            if (userId == expense.payerId) {
                                continue
                            }

                            // Create an ExpenseSplit with groupId and owedTo
                            val expenseSplit = ExpenseSplit(
                                expenseId = expense.id,
                                userId = userId,
                                groupId = groupId,  // Set the groupId for each split
                                owedTo = expense.payerId,  // Set owedTo as the ID of the expense creator
                                amount = splitAmount,
                                hasPaid = false
                            )

                            firestore.collection("expense_splits").add(expenseSplit)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error fetching members: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private inner class GroupPagerAdapter(fa: FragmentActivity, private val groupId: String?) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> GroupOverviewFragment().apply {
                    arguments = Bundle().apply { putString("groupId", groupId) }
                }
                1 -> SummaryFragment().apply {
                    arguments = Bundle().apply { putString("groupId", groupId) }
                }
                else -> throw IllegalArgumentException("Invalid tab position")
            }
        }
    }
}
