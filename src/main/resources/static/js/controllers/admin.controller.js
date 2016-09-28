app.controller('AdminController', function ($scope, $http, $state, $timeout, AdminService, FileUploader) {
    $scope.resourcesLoaded = false;
    $scope.maxSize = 5;  // pagination size
    $scope.itemsPerPage = 10;

    $scope.notValidated = {
        users: [],
        currentPage: 1,
        totalItems: 0
    };

    $scope.notValidated.getData = function () {
        var self = this;
        AdminService.getNotValidated(self.currentPage, $scope.itemsPerPage).then(function (response) {
            self.users = response.content;
            self.totalItems = response.totalElements;
        });
    };

    $scope.notValidated.needPagination = function() {
        return this.totalItems > $scope.itemsPerPage;
    };

    $scope.validated = {
        users: [],
        currentPage: 1,
        totalItems: 0
    };

    $scope.validated.getData = function () {
        var self = this;
        AdminService.getValidated(self.currentPage, $scope.itemsPerPage).then(function (response) {
            self.users = response.content;
            self.totalItems = response.totalElements;
        });
    };

    $scope.validated.needPagination = function() {
        return this.totalItems > $scope.itemsPerPage;
    };

    $scope.notValidated.getData();
    $scope.validated.getData();
    $scope.resourcesLoaded = true;

    var confirmValidation = function () {
        return confirm("Approving a user means you cannot delete them anymore!");
    };

    $scope.acceptUser = function (id) {
        if (confirmValidation()) {
            AdminService.validateUser(id).then(function () {
                $scope.notValidated.getData();
            });
        }
    };

    var confirmDeletion = function () {
        return confirm("Once you delete a user, there is no going back!");
    };

    $scope.deleteUser = function (id) {
        if (confirmDeletion()) {
            AdminService.deleteUser(id).then(function () {
                $scope.notValidated.getData();
            });
        }
    };

    $scope.uploader = new FileUploader({
        url: '/api/admin/uploadBackup',
        removeAfterUpload: true
    });

    $scope.uploader.onCompleteAll = function() {
        $scope.uploaded = true;
    };

    $scope.uploader.onErrorItem = function() {
        $scope.uploader.cancelAll();
        $scope.uploaded = false;
    };

    $scope.downloadDump = function () {
        $http.get('/api/admin/dumpDatabase', {
            transformResponse: angular.identity
        }).then(function (response) {
            var text = response.data;
            var filename = "dump.xml";
            var blob = new Blob([text], {type: "application/xml;charset=utf-8"});
            saveAs(blob, filename);
        });
    };

    $scope.runAutosuggestions = function() {
        $http.get('/api/admin/runAutosuggestions').then(function() {
            $scope.algorithmRunning = true;
            $timeout(function() {$scope.algorithmRunning = false;}, 2000);
        });
    }
});