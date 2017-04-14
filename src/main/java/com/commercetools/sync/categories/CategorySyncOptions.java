package com.commercetools.sync.categories;

import com.commercetools.sync.commons.BaseOptions;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.impl.CategoryServiceImpl;
import io.sphere.sdk.categories.Category;
import io.sphere.sdk.commands.UpdateAction;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CategorySyncOptions extends BaseOptions {
    private CategoryService categoryService;
    public CategorySyncOptions(@Nonnull final String ctpProjectKey,
                               @Nonnull final String ctpClientId,
                               @Nonnull final String ctpClientSecret,
                               @Nonnull final BiConsumer<String, Throwable> updateActionErrorCallBack,
                               @Nonnull final Consumer<String> updateActionWarningCallBack) {
        super(ctpProjectKey, ctpClientId, ctpClientSecret, updateActionErrorCallBack, updateActionWarningCallBack);
    }

    // optional filter which can be applied on generated list of update actions
    private Function<List<UpdateAction<Category>>, List<UpdateAction<Category>>> filterActions() {
        return updateActions -> null;
    }

    public CategoryService getCategoryService() {
        if (categoryService == null) {
            categoryService = new CategoryServiceImpl(getCTPclient());
        }
        return categoryService;
    }
}
