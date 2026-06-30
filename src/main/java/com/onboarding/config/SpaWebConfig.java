package com.onboarding.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the bundled Angular SPA (copied into {@code classpath:/static/} at build time) and
 * falls back to {@code index.html} for client-side routes so deep links / refreshes work.
 *
 * <p>{@code @RestController} mappings for {@code /api/**} take precedence over this resource
 * handler, so the API is unaffected. Unknown {@code /api/**} paths are still allowed to 404
 * rather than being masked by the SPA index.
 */
@Component
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath,
                                                   @NonNull Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Let the API and real static assets 404 normally instead of returning the SPA shell.
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("health")) {
                            return null;
                        }
                        // Any other unresolved path is an Angular client-side route -> serve the SPA shell.
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
