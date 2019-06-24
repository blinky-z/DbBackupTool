window.setInterval(function () {
    $.ajax(
        {
            url: '/api/get-tasks',
            type: 'GET',
            success: function (data, textStatus, jqXHR) {
                var response = JSON.parse(jqXHR.responseText);

                var bodyHtml = "";

                for (var currentBackupTask = 0; currentBackupTask < response.length; currentBackupTask++) {
                    var backupTask = response[currentBackupTask];

                    bodyHtml += '<tr>\n' +
                        '<th scope="row">' + backupTask.type + '</th>\n' +
                        '<td>' + backupTask.state + '</td>\n' +
                        '<td>' + backupTask.error + '</td>\n' +
                        '<td>' + backupTask.interrupted + '</td>\n' +
                        '<td>' + backupTask.time + '</td>\n' +
                        '<td>\n' +
                        '    <div class="d-flex flex-row-reverse bd-highlight">\n' +
                        '        <form th:action="@{/cancel-task}" th:method="post">\n' +
                        '            <input hidden th:name="taskId" th:value="*{id}">\n' +
                        '            <button class="btn btn-secondary" type="submit">\n' +
                        '                Cancel\n' +
                        '            </button>\n' +
                        '        </form>\n' +
                        '    </div>\n' +
                        '</td>' +
                        '</tr>\n';
                }

                if (bodyHtml !== "") {
                    var backupTasksTableBody = $('#backupTasksTableBody');
                    backupTasksTableBody.html(bodyHtml);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log("Error updating backup states. Error code: " + jqXHR.status);
            }
        }
    );

}, 5000);