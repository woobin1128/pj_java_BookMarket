package com.market.bookitem;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 카카오 도서 검색에서 가져온 외부 도서의 ID 접두어 */
    public static final String EXTERNAL_PREFIX = "KB-";

    private String bookId;
    private String name;
    private int unitPrice;
    private String author;
    private String description;
    private String publisher;
    private String category;
    private long unitsInStock;
    private String releaseDate;
    private String condition;
    private String filename;
    private String imageFileName;
    private Set<String> tabs = new LinkedHashSet<>();

    public Book() {
        super();
    }

    public Book(String bookId, String name, int unitPrice) {
        this.bookId = bookId;
        this.name = name;
        this.unitPrice = unitPrice;
    }

    // DataStore.loadSeedBooks에서 호출하는 8인자 생성자
    public Book(String bookId, String name, int unitPrice, String author, String description,
                String category, String releaseDate, String imageFileName) {
        this.bookId = bookId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.author = author;
        this.description = description;
        this.category = category;
        this.releaseDate = releaseDate;
        this.imageFileName = imageFileName;
        this.filename = imageFileName;
    }

    public boolean hasTab(String tab) {
        if (tab == null || tabs == null) return false;
        return tabs.contains(tab.toUpperCase());
    }

    public void addTab(String tab) {
        if (tab == null || tab.trim().isEmpty()) return;
        if (tabs == null) tabs = new LinkedHashSet<>();
        tabs.add(tab.trim().toUpperCase());
    }

    public boolean isExternal() {
        return bookId != null && bookId.startsWith(EXTERNAL_PREFIX);
    }

    public Set<String> getTabs() {
        return tabs;
    }

    public void setTabs(Set<String> tabs) {
        this.tabs = (tabs != null) ? new LinkedHashSet<>(tabs) : new LinkedHashSet<>();
    }

    public String getImageFileName() {
        return (imageFileName != null && !imageFileName.isEmpty()) ? imageFileName : filename;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
        this.filename = imageFileName;
    }

    public String getImageWebPath(String contextPath) {
		String img = getImageFileName();
		String ctx = (contextPath == null) ? "" : contextPath;
		if (img == null || img.trim().isEmpty()) {
			return ctx + "/images/default_book.png";
		}
		String clean = img.trim();
		if (clean.startsWith("http://") || clean.startsWith("https://")) {
			return clean.startsWith("http://") ? "https://" + clean.substring(7) : clean;   // 외부 표지(카카오 CDN)
		}
		if (clean.startsWith("/")) {
			clean = clean.substring(1);
		}
		if (clean.startsWith("uploads/")) {
			return ctx + "/" + clean;
		}
		if (clean.startsWith("images/")) {
			return ctx + "/" + clean;
		}
		return ctx + "/images/" + clean;
	}

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getUnitsInStock() { return unitsInStock; }
    public void setUnitsInStock(long unitsInStock) { this.unitsInStock = unitsInStock; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) {
        this.filename = filename;
        if (this.imageFileName == null) {
            this.imageFileName = filename;
        }
    }
}