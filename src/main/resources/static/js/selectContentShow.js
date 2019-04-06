$(document).ready(function () {
        $('#barTabs a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            localStorage.setItem('activeTab', $(e.target).attr('href'));
        });

        var activeTab = localStorage.getItem('activeTab');
        if (activeTab) {
            $('#barTabs a[href="' + activeTab + '"]').tab('show');
        }
    }
);

$(document).ready(function () {
    $("#configureStorageForm").change(function () {
        $(this).find("option:selected").each(function () {
            var optionValue = $(this).attr("id");
            var $selectStorageContent = $("#selectStorageContent");
            var $commonStorageInputFields = $('#commonStorageInputFields');
            if (optionValue) {
                $commonStorageInputFields.show();
                $selectStorageContent.find(".box").not("." + optionValue).hide();
                $selectStorageContent.find("." + optionValue).show();
            } else {
                $commonStorageInputFields.hide();
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