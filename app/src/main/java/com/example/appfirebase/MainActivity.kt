package com.example.appfirebase

import android.os.Bundle
import android.util.Log.w
import android.util.Log.d
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.appfirebase.ui.theme.AppFirebaseTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    // Instancia o Firestore
    private val db: FirebaseFirestore by lazy {
        Firebase.firestore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()
        setContent {
            AppFirebaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(db)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(db: FirebaseFirestore) {
    var nome by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var clientes by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var editandoClienteId by remember { mutableStateOf<String?>(null) }
    var telefoneErro by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }

    // Função para limpar campos de entrada
    fun limparCampos() {
        nome = ""
        telefone = ""
        email = ""
        editandoClienteId = null
    }

    // Função para ler clientes do Firestore
    fun lerClientes() {
        db.collection("Clientes")
            .get()
            .addOnSuccessListener { documents ->
                clientes = documents.map { document ->
                    Cliente(
                        id = document.id,
                        nome = document.getString("nome") ?: "",
                        telefone = document.getString("telefone") ?: "",
                        email = document.getString("email") ?: ""
                    )
                }
            }
            .addOnFailureListener { e ->
                w("Firestore", "Error getting documents.", e)
            }
    }

    // Função para excluir um cliente
    fun excluirCliente(clienteId: String) {
        db.collection("Clientes").document(clienteId).delete()
            .addOnSuccessListener {
                d("Firestore", "Cliente excluído com sucesso!")
                lerClientes()
            }
            .addOnFailureListener { e ->
                w("Firestore", "Erro ao excluir documento", e)
            }
    }

    // Função para editar um cliente
    fun editarCliente(clienteId: String, novoNome: String, novoTelefone: String, novoEmail: String) {
        val clienteAtualizado = hashMapOf(
            "nome" to novoNome,
            "telefone" to novoTelefone,
            "email" to novoEmail
        )
        db.collection("Clientes").document(clienteId).set(clienteAtualizado)
            .addOnSuccessListener {
                d("Firestore", "Cliente atualizado com sucesso!")
                lerClientes()
                limparCampos()
            }
            .addOnFailureListener { e ->
                w("Firestore", "Erro ao atualizar documento", e)
            }
    }

    // Função para validar o telefone
    fun validarTelefone(telefone: String): Boolean {
        return telefone.all { it.isDigit() } && telefone.length >= 8
    }

    LaunchedEffect(Unit) {
        lerClientes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Gestão de Clientes", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exibe quantidade de clientes com destaque
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Total de clientes cadastrados: ${clientes.size}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Nome")
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = telefone,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) {
                        telefone = it
                        telefoneErro = !validarTelefone(it)
                    }
                },
                isError = telefoneErro,
                label = { Text("Telefone") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Telefone")
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone)
            )

            if (telefoneErro) {
                Text(
                    text = "Número de telefone inválido",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email")
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ElevatedButton(
                    onClick = {
                        if (telefoneErro) return@ElevatedButton

                        salvando = true

                        if (editandoClienteId != null) {
                            editarCliente(editandoClienteId!!, nome, telefone, email)
                        } else {
                            val pessoas = hashMapOf(
                                "nome" to nome,
                                "telefone" to telefone,
                                "email" to email
                            )
                            db.collection("Clientes").add(pessoas)
                                .addOnSuccessListener { documentReference ->
                                    d("Firestore", "Documento adicionado com ID: ${documentReference.id}")
                                    lerClientes()
                                    limparCampos()
                                }
                                .addOnFailureListener { e ->
                                    w("Firestore", "Erro ao adicionar documento", e)
                                }
                                .addOnCompleteListener {
                                    salvando = false
                                }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (editandoClienteId != null) "Atualizar Cliente" else "Cadastrar Cliente",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ElevatedButton(
                    onClick = { limparCampos() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(text = "Limpar", color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (salvando) {
                CircularProgressIndicator()
                LaunchedEffect(salvando) {
                    delay(3000) // Tempo de exibição do "Salvando..."
                    salvando = false
                }
                Text(text = "Salvando...", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            }

            if (clientes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum cliente cadastrado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF5E5D5D)
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(clientes) { cliente ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(120.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(text = "Nome: ${cliente.nome}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Telefone: ${cliente.telefone}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "Email: ${cliente.email}", style = MaterialTheme.typography.bodyLarge)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(onClick = {
                                        nome = cliente.nome
                                        telefone = cliente.telefone
                                        email = cliente.email
                                        editandoClienteId = cliente.id
                                    }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                                    }

                                    IconButton(onClick = {
                                        excluirCliente(cliente.id)
                                    }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Cliente(
    val id: String,
    val nome: String,
    val telefone: String,
    val email: String
)
