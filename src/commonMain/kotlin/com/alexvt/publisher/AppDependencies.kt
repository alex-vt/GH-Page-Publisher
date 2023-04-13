package com.alexvt.publisher

import com.alexvt.publisher.repositories.CacheStorageRepository
import com.alexvt.publisher.repositories.FileAccessRepository
import com.alexvt.publisher.repositories.GitRepository
import com.alexvt.publisher.repositories.LogRepository
import com.alexvt.publisher.repositories.NewPageRepository
import com.alexvt.publisher.repositories.SettingsStorageRepository
import com.alexvt.publisher.viewmodels.MainViewModelUseCases
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

@AppScope
@Component
abstract class AppDependencies {

    abstract val mainViewModelUseCases: MainViewModelUseCases

    @AppScope
    @Provides
    protected fun newPageRepository(): NewPageRepository =
        NewPageRepository()

    @AppScope
    @Provides
    protected fun settingsStorageRepository(): SettingsStorageRepository =
        SettingsStorageRepository()

    @AppScope
    @Provides
    protected fun cacheStorageRepository(): CacheStorageRepository =
        CacheStorageRepository()

    @AppScope
    @Provides
    protected fun getFilesAccessRepository(): FileAccessRepository =
        FileAccessRepository()

    @AppScope
    @Provides
    protected fun getGitRepository(): GitRepository =
        GitRepository()

    @AppScope
    @Provides
    protected fun getLogsRepository(): LogRepository =
        LogRepository()

}
