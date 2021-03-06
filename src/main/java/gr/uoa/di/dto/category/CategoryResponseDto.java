package gr.uoa.di.dto.category;

import gr.uoa.di.dto.item.ItemResponseDto;

import java.util.List;

public class CategoryResponseDto {
    private int id;
    private String name;
    private long count;
    private CategoryResponseDto parent;
    private List<CategoryResponseDto> subcategories;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public CategoryResponseDto getParent() {
        return parent;
    }

    public void setParent(CategoryResponseDto parent) {
        this.parent = parent;
    }

    public List<CategoryResponseDto> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<CategoryResponseDto> subcategories) {
        this.subcategories = subcategories;
    }
}
