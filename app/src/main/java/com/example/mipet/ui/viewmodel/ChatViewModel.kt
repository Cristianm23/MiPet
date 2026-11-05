package com.example.mipet.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mipet.data.model.ChatMessage
import com.example.mipet.data.model.Resource
import com.example.mipet.data.model.User
import com.example.mipet.data.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _usersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading())
    val usersState = _usersState.asStateFlow()

    private val _messagesState = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesState = _messagesState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _usersState.value = Resource.Loading()
            _usersState.value = repository.getRegisteredUsers()
        }
    }

    fun listenToMessages(partnerUid: String) {
        viewModelScope.launch {
            repository.getChatMessages(partnerUid).collect { messages ->
                _messagesState.value = messages
            }
        }
    }

    fun sendMessage(partnerUid: String, text: String, senderName: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(partnerUid, text, senderName)
        }
    }
}
