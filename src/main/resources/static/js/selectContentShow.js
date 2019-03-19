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
            if (optionValue) {
                $('#inputDatabaseCommonFields').show();
                $selectStorageContent.find(".box").not("." + optionValue).hide();
                $selectStorageContent.find("." + optionValue).show();
            } else {
                $('#inputDatabaseCommonFields').hide();
                $selectStorageContent.find(".box").hide();
            }
        });
    }).change()
});