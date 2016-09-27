app.controller('ProfileAuctionsActiveController', function ($scope, $http, $stateParams, $interval, AuthService) {
    $scope.$parent.view_tab = "active";
    $scope.maxSize = 5;  // pagination size
    $scope.itemsPerPage = 5;
    $scope.currentPage = 1;
    $scope.totalItems =  0;
    $scope.items = [];
    $scope.filteredItems = [];

    var getItems = function() {
        $http.get('/api/items/active/' + AuthService.user.user, {
            params: {
                page: $scope.currentPage - 1,
                size: $scope.itemsPerPage
            }
        }).then(function success(response) {
            $scope.items = response.data.content.map(function (item) {
                if (item.buyprice) {
                    item.buyprice = +item.buyprice / 100;
                }
                item.currentbid = +item.currentbid / 100;
                item.firstbid = +item.firstbid / 100;
                item.endOffset = moment(item.endDate).fromNow();
                return item;
            });
            $scope.totalItems = response.data.totalElements;
            $scope.filteredItems = $scope.items.slice(0, $scope.itemsPerPage);
        });
    };

    getItems();
    var updateInterval = $interval(getItems, 5000);


    $scope.needPagination = function() {
        return $scope.totalItems > $scope.itemsPerPage;
    };

    $scope.$watch("currentPage + itemsPerPage", function() {
        var begin = (($scope.currentPage - 1) * $scope.itemsPerPage)
            , end = begin + $scope.itemsPerPage;

        $scope.filteredItems = $scope.items.slice(begin, end);
    });

    $scope.canEdit = function(item) {
        return item.firstbid == item.currentbid;
    };

    $scope.deleteItem = function (itemId) {
        if (confirm('Are you sure you want to delete this item?')) {
            $http.delete('/api/items/' + itemId).then(function () {
                $scope.getItems();
            })
        }
    };

    $scope.$on('$stateChangeStart', function () {
        if (updateInterval) {
            $interval.cancel(updateInterval);
            updateInterval = null;
        }
    });
});