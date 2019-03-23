$(document).ready(function () {
    $("#configureStorageForm").change(function () {
        $(this).find("option:selected").each(function () {
            var optionValue = $(this).attr("id");
            var $selectStorageContent = $("#selectStorageContent");
            if (optionValue) {

                $selectStorageContent.find(".box").not("." + optionValue).hide();
                $selectStorageContent.find("." + optionValue).show();
            } else {
                $selectStorageContent.find(".box").hide();
            }
        });
    }).change()
});

$(document).ready(function () {
    $("#configureDatabaseForm").change(function () {
        $(this).find("option:selected").each(function () {
            var optionValue = $(this).attr("id");
            var $selectStorageContent = $("#selectDatabaseContent");
            var $commonDatabaseInputFields = $('#commonDatabaseInputFields');
            if (optionValue) {
                $commonDatabaseInputFields.show();
                $selectStorageContent.find(".box").not("." + optionValue).hide();
                $selectStorageContent.find("." + optionValue).show();
            } else {
                $commonDatabaseInputFields.hide();
                $selectStorageContent.find(".box").hide();
            }
        });
    }).change()
});