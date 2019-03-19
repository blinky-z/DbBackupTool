function removeDatabase(databaseItemRemoveButton) {
    var databaseItem = $(databaseItemRemoveButton).closest(".databaseItem");
    var databaseId = databaseItem.attr("data-id");
    var databaseType = databaseItem.attr("data-database-type");

    $.ajax({
        url: "/database",
        type: "delete",
        data: {
            id: databaseId,
            databaseType: databaseType
        },
        success: function (response) {
            alert("Database was successfully deleted");
            location.reload();
        },
        error: function (xhr) {
            alert("Database deletion error");
        }
    });
}

function removeStorage(storageItemRemoveButton) {
    var storageItem = $(storageItemRemoveButton).closest(".storageItem");
    var storageId = storageItem.attr("data-id");
    var storageType = storageItem.attr("data-storage-type");

    $.ajax({
        url: "/storage",
        type: "delete",
        data: {
            id: storageId,
            storageType: storageType
        },
        success: function (response) {
            alert("Storage was successfully deleted");
            location.reload();
        },
        error: function (xhr) {
            alert("Storage deletion error");
        }
    });
}