var dashboardApp = angular.module('dashboardApp', ['angular-hal']);

dashboardApp.controller('DashboardCtrl', function ($log, $timeout, $scope) {

   $scope.footerMsg = {msg: null, success: null, error: null, errors: null, promise: null};

   $scope.showFooterMsg = function (m) {
      $scope.footerMsg.msg = m.msg || null;
      $scope.footerMsg.success = m.success || null;
      $scope.footerMsg.error = m.error || null;
      $scope.footerMsg.errors = m.errors || null;

      if ($scope.footerMsg.promise) {
         $timeout.cancel($scope.footerMsg.promise);
         $scope.footerMsg.promise = null;
      }

      if (m.msg) {
         return;
      }

      $scope.footerMsg.promise = $timeout(function () {
         $scope.footerMsg = {msg: null, success: null, error: null, errors: null, promise: null};
      }, $scope.footerMsg.error ? 8000 : 2000, true);
   }

});

dashboardApp.controller('AccountCtrl', function ($log, $window, $filter, $timeout, $scope, halClient) {

   var emptyAccount = {username: null, firstName: null, lastName: null, email: null, password: null, roles: null};
   var edited = {changed: false, row: null, value: null, promise: null};

   function showErrorMsg(action, error) {
      var errMsg = action + ": FAILED! " + error.status + " " + error.statusText;
      var errors = null;
      if (error.data && error.data.errors) {
         errors = error.data.errors;
      }
      console.error(errMsg);
      $scope.showFooterMsg({error: errMsg, errors: errors});
   }

   function accountPage(pageNumber) {
      var action = "Getting page " + pageNumber;

      if (!!edited.promise) {
         $timeout.cancel(edited.promise);
         edited.promise = null;
      }

      $scope.serviceResource.$get('accounts', {'page': pageNumber, 'size': 5, sort: 'created'})
          .then(function (resource) {
             $scope.page = resource.page;
             return resource.$get('accounts');
          }, function (error) {
             showErrorMsg(action, error);
          })
          .then(function (resource) {
             $scope.accountResource = resource;
             $scope.accounts = angular.copy(resource);
          });
   }

   $scope.editorHandler = function (event, row) {
      var elem = event.currentTarget;
      var type = event.type;

      if (type == "focus") {
         if (!!edited.promise) {
            $timeout.cancel(edited.promise);
            edited.promise = null;
         }

         if (row != edited.row && edited.changed) {
            edited.changed = false;
            $scope.updateAccount(edited.row, $scope.accounts[edited.row]);
         }

         edited.value = elem.value;
         edited.row = row;
         return;
      }

      if (type == "blur") {
         edited.changed = edited.changed || elem.value !== edited.value;
         if (edited.changed) {
            edited.promise = $timeout(function () {
               edited.changed = false;
               $scope.updateAccount(row, $scope.accounts[row]);
            }, 333, true);
         }
      }
   };

   $scope.deleteHandler = function (event, row) {
      if ($window.confirm('Are you sure you want to delete the account?')) {
         $scope.deleteAccount(row, function () {
            var page = $scope.page;
            var p = ((page.totalElements-1)%page.size != 0) ? page.number : page.number-1;
            accountPage(p);
         });
      }
   };

   $scope.createHandler = function (account) {
      account.roles = "USER";
      $scope.createAccount(account, function () {
         var page = $scope.page;
         var p = parseInt((page.totalElements)/page.size);
         accountPage(p);
         $scope.account = angular.copy(emptyAccount);
      });
   };

   // PUT/PATCH Bug in Spring Data Rest using Hibernate: DATAREST-441
   $scope.updateAccount = function (row, data) {
      var action = "Account update";
      $scope.showFooterMsg({msg: action + "..."});
      $scope.accountResource[row].$patch('self', {}, data)
          .then(function () {
             $scope.showFooterMsg({success: action + ": SUCCESSFUL!"});
          }, function (error) {
             showErrorMsg(action, error);
          });
   };

   $scope.createAccount = function (account, successCallback) {
      var action = "Account creation";
      $scope.showFooterMsg({msg: action});
      $scope.serviceResource.$post('accounts', {}, account)
          .then(function () {
             $scope.showFooterMsg({success: action + ": SUCCESSFUL!"});
             if (successCallback) {
                successCallback();
             }
          }, function (error) {
             showErrorMsg(action, error);
          });
   };

   $scope.deleteAccount = function (row, successCallback) {
      var action = "Account deletion";
      $scope.showFooterMsg({msg: action + "..."});
      $scope.accountResource[row].$delete('self', {})
          .then(function () {
             $scope.showFooterMsg({success: action + ": SUCCESSFUL!"});
             if (successCallback) {
                successCallback();
             }
          }, function (error) {
             showErrorMsg(action, error);
          });
   };


   halClient.$get('/service').then(function (resource) {
      $scope.serviceResource = resource;
      accountPage(0);
   });

   $scope.loadAccountPage = function (pageNumber) {
      accountPage(pageNumber % $scope.page.totalPages);
   };

});
