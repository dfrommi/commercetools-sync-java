package com.commercetools.sync.categories;


import com.commercetools.sync.commons.helpers.CtpClient;
import com.commercetools.sync.services.CategoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.categories.CategoryDraft;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.models.SphereException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;

import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryDraft;
import static com.commercetools.sync.categories.CategorySyncMockUtils.getMockCategoryService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySyncTest {
    private CategorySync categorySync;
    private CategorySyncOptions categorySyncOptions;

    /**
     * Initializes instances of  {@link CategorySyncOptions} and {@link CategorySync} which will be used by some
     * of the unit test methods in this test class.
     */
    @Before
    public void setup() {
        final SphereClientConfig clientConfig = SphereClientConfig.of("testPK", "testCI", "testCS");
        final CtpClient ctpClient = mock(CtpClient.class);
        when(ctpClient.getClientConfig()).thenReturn(clientConfig);
        categorySyncOptions = CategorySyncOptionsBuilder.of(ctpClient)
            .build();
        categorySync = new CategorySync(categorySyncOptions, mock(TypeService.class),
                                        getMockCategoryService());
    }

    @Test
    public void syncDrafts_WithEmptyListOfDrafts_ShouldNotProcessAnyCategories() {
        categorySync.syncDrafts(new ArrayList<>());

        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 0 categories were processed in total "
                + "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithANullDraft_ShouldSkipIt() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(null);

        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 0 categories were processed in total "
                + "(0 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithADraftWithNoSetExternalID_ShouldFailSync() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "noExternalIdDraft", "no-external-id-draft", null));

        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithNoExistingCategory_ShouldCreateCategory() {
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString())).thenReturn(null);
        final CategorySync categorySync = new CategorySync(categorySyncOptions, mock(TypeService.class),
                                                           categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "newExternalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(1 created, 0 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithExistingCategory_ShouldUpdateCategory() {
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 1 updated and 0 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithExistingCategoryButExceptionOnFetch_ShouldFailSync() {
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString())).thenThrow(new SphereException());
        final CategorySync categorySync = new CategorySync(categorySyncOptions, mock(TypeService.class),
                                                           categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithNoExistingCategoryButExceptionOnCreate_ShouldFailSync() {
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.fetchCategoryByExternalId(anyString())).thenReturn(null);
        when(categoryService.createCategory(any())).thenThrow(new SphereException());
        final CategorySync categorySync = new CategorySync(categorySyncOptions, mock(TypeService.class),
                                                           categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
    }

    @Test
    public void syncDrafts_WithExistingCategoryButExceptionOnUpdate_ShouldFailSync() {
        final CategoryService categoryService = getMockCategoryService();
        when(categoryService.updateCategory(any(), any())).thenThrow(new SphereException());
        final CategorySync categorySync = new CategorySync(categorySyncOptions, mock(TypeService.class),
                                                           categoryService);
        final ArrayList<CategoryDraft> categoryDrafts = new ArrayList<>();
        categoryDrafts.add(getMockCategoryDraft(Locale.ENGLISH, "name", "slug", "externalId"));


        categorySync.syncDrafts(categoryDrafts);
        assertThat(categorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(categorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(categorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 categories were processed in total "
                + "(0 created, 0 updated and 1 categories failed to sync).");
    }

}