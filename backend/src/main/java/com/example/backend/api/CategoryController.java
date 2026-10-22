package com.example.backend.api;

import com.example.backend.service.CategoryService;
import com.example.backend.service.CategoryService.CategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Danh mục")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name) { }

    @Operation(summary = "Danh sách danh mục")
    @SecurityRequirements
    @GetMapping
    public List<CategoryView> list() {
        return categories.list();
    }

    @Operation(summary = "Admin thêm danh mục")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView create(@Valid @RequestBody CategoryRequest request) {
        return categories.create(request.name(), CurrentUser.get());
    }

    @Operation(summary = "Admin đổi tên danh mục")
    @RequestMapping(path = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public CategoryView rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categories.rename(id, request.name(), CurrentUser.get());
    }

    @Operation(summary = "Admin xóa danh mục chưa được dùng")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categories.delete(id, CurrentUser.get());
    }
}
