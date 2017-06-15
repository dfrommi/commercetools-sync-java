package com.commercetools.sync.products;

import com.commercetools.sync.commons.BaseSync;
import com.commercetools.sync.products.actions.ProductUpdateActionsBuilder;
import com.commercetools.sync.products.helpers.ProductSyncStatistics;
import com.commercetools.sync.services.ProductService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.ProductCatalogData;
import io.sphere.sdk.products.ProductDraft;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class ProductSync extends BaseSync<ProductDraft, ProductSyncStatistics, ProductSyncOptions> {

    private final ProductService service;
    private final ProductUpdateActionsBuilder updateActionsBuilder;

    public ProductSync(final ProductSyncOptions productSyncOptions) {
        this(productSyncOptions, ProductService.of(productSyncOptions.getCtpClient()),
            ProductUpdateActionsBuilder.of());
    }

    ProductSync(final ProductSyncOptions productSyncOptions, final ProductService service,
                final ProductUpdateActionsBuilder updateActionsBuilder) {
        super(new ProductSyncStatistics(), productSyncOptions);
        this.service = service;
        this.updateActionsBuilder = updateActionsBuilder;
    }

    @Override
    protected CompletionStage<ProductSyncStatistics> process(@Nonnull final List<ProductDraft> resourceDrafts) {
        for (ProductDraft productDraft : resourceDrafts) {
            try {
                CompletionStage<Optional<Product>> fetch = service.fetch(productDraft.getKey());
                fetch.thenCompose(productOptional -> productOptional
                    .map(product -> syncProduct(product, productDraft))
                    .orElseGet(() -> createProduct(productDraft))
                ).toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }
            statistics.incrementProcessed();
        }
        return completedFuture(statistics);
    }

    private CompletionStage<Void> createProduct(final ProductDraft productDraft) {
        return publishIfNeeded(service.create(productDraft))
            .thenRun(statistics::incrementCreated);
    }

    @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
    private CompletionStage<Void> syncProduct(final Product product, final ProductDraft productDraft) {
        List<UpdateAction<Product>> updateActions =
            updateActionsBuilder.buildActions(product, productDraft, syncOptions);
        if (!updateActions.isEmpty()) {
            return publishIfNeeded(service.update(product, updateActions))
                .thenRun(statistics::incrementUpdated);
        }
        return publishIfNeeded(completedFuture(product)).thenApply(p -> null);
    }

    private CompletionStage<Product> publishIfNeeded(final CompletionStage<Product> productStage) {
        if (syncOptions.shouldPublish()) {
            return productStage.thenCompose(product -> {
                ProductCatalogData data = product.getMasterData();
                if (!data.isPublished() || data.hasStagedChanges()) {
                    return service.publish(product);
                }
                return productStage;
            });
        }
        return productStage;
    }

}
