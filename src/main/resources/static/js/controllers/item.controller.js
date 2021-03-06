app.controller('ItemController', function ($scope, $http, $state, $stateParams, $interval, $timeout, leafletData, AuthService) {
    var bidsInterval;

    $scope.item = {name: $stateParams.itemName};
    $scope.loggedIn = AuthService.isLoggedIn();
    $scope.currentUser = AuthService.user.user;

    $http.get('api/items/' + $stateParams.itemId).then(function (response) {
        var item = response.data;
        if (item.buyprice) {
            item.buyprice = +item.buyprice / 100;
        }
        item.currentbid = +item.currentbid / 100;
        $scope.item = item;
        $scope.breadcrumb = [];
        var parent = item.category;
        while (parent != null) {
            $scope.breadcrumb.unshift(parent);
            parent = parent.parent;
        }

        $scope.item.realEndDate = new Date(item.endDate);

        if (item.lat && item.lon) {
            angular.extend($scope, {
                markers: {
                    item: {
                        lat: item.lat,
                        lng: item.lon
                    }
                }
            });
            $scope.$on('leafletDirectiveMap.map.layeradd', function () {
                leafletData.getMap().then(function (map) {
                    map.setView([item.lat, item.lon], 5);
                });
            });
        }

        updateBids();
        bidsInterval = $interval(updateBids, 3000);
    }, function() {
        $state.go('page_not_found');
    });

    $scope.$on('$stateChangeStart', function () {
        if (bidsInterval) {
            $interval.cancel(bidsInterval);
            bidsInterval = null;
        }
    });

    /* update the time remaining/passed from the auction end */
    var updateOffset = function () {
        var endMoment = moment($scope.item.realEndDate);
        if (!$scope.item.finished) {
            $scope.item.finished = new Date() > $scope.item.realEndDate;
        }
        $scope.end = ($scope.item.finished ? "Auction ended:" : "Auction ends:");
        $scope.endOffset = endMoment.fromNow();
        $scope.endTime = endMoment.format('DD/MM/YYYY HH:mm');
    };

    /* continuously refresh the bids */
    var updateBids = function () {
        $http.get('api/bids/' + $stateParams.itemId).then(function (response) {
            $scope.bids = response.data.map(function (bid) {
                bid.amount = (+bid.amount) / 100;
                bid.date = new Date(bid.time);
                bid.timeOffset = moment(bid.date).fromNow();
                return bid;
            });

            if ($scope.bids.length) {
                var lastBid = $scope.bids.reduce(function (a, b) {
                    return a.amount > b.amount ? a : b;
                });
                if ($scope.item.buyprice && lastBid.amount >= $scope.item.buyprice) {
                    $scope.item.realEndDate = lastBid.date;
                }
                if (lastBid.amount > $scope.item.currentbid) {
                    $scope.item.currentbid = lastBid.amount;
                }
            }

            var scroll = $('.bid-scroll');
            updateOffset();

            $timeout(function () {
                $('[data-toggle="tooltip"]').tooltip();
                scroll.animate({scrollTop: scroll.children().height()});
            }, 0);

            if ($scope.item.finished) {
                $interval.cancel(bidsInterval);
                bidsInterval = null;
            }
        });
    };

    $scope.canBid = function () {
        return $scope.loggedIn && !$scope.item.finished && new Date() > new Date($scope.item.startDate) &&
            $scope.currentUser != $scope.item.sellerUsername
    };

    $scope.canEdit = function() {
        return $scope.currentUser == $scope.item.sellerUsername && $scope.bids && $scope.bids.length == 0;
    };

    var confirmBid = function () {
        return confirm("Are you happy with this bid?");
    };

    $scope.submitBid = function (amount) {
        if (confirmBid()) {
            var amountStr = Math.floor(amount * 100);
            $http.post('api/bids/' + $stateParams.itemId, amountStr).then(function () {
                $scope.bidError = null;
                $scope.bidAmount = null;
                updateBids();
            }, function (err) {
                $scope.bidError = err.data.message;
            });
        }
    };

    $scope.deleteItem = function (itemId) {
        if (confirm('Are you sure you want to delete this item?')) {
            $http.delete('api/items/' + itemId).then(function() {
                $state.go('main.category', {
                    categoryId: $scope.item.category.id,
                    categoryName: $scope.item.category.name
                });
            });
        }
    };
});
