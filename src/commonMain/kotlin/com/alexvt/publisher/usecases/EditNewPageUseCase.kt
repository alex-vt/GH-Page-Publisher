package com.alexvt.publisher.usecases

import com.alexvt.publisher.AppScope
import com.alexvt.publisher.repositories.NewPageRepository
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class EditNewPageUseCase(
    private val newPageRepository: NewPageRepository,
) {

    fun execute(newText: String) {
        newPageRepository.set(newText)
    }

}
