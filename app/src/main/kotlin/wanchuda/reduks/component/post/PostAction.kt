package wanchuda.reduks.component.post

import com.beyondeye.reduks.Action
import com.beyondeye.reduks.NextDispatcher
import com.beyondeye.reduks.StandardAction
import com.beyondeye.reduks.Thunk
import wanchuda.reduks.common.action.ApiAction
import wanchuda.reduks.common.action.DbAction
import wanchuda.reduks.common.separator.ApiResponseType
import wanchuda.reduks.common.separator.ApiState
import wanchuda.reduks.common.separator.DbState
import wanchuda.reduks.component.app.AppState
import wanchuda.reduks.model.Post

sealed class PostAction(override val payload: Any? = null,
                        override val error: Boolean = false) : StandardAction {

    //================================================================================
    // region action general
    //================================================================================

    class FetchPostList() : PostAction(), Thunk<AppState> {
        override fun execute(dispatcher: NextDispatcher, state: AppState): Action {
            val onApiRequesting: (Any?) -> Action = { payload ->
                dispatcher.dispatch(DbAction.QueryList(klass = Post::class,
                                                       nextAction = { payload ->
                                                           PostListQueryFromDb(payload = payload as List<Post>)
                                                           //= UpdatePostList(payload = payload as List<Post>, apiState = ApiState.UNCHANGED, dbState = DbState.SUCCESS)
                                                       }))
                PostListApiRequesting()
                //= UpdatePostList(payload = null, apiState = ApiState.REQUESTING, dbState = DbState.UNCHANGED)
            }

            val onApiSuccess: (Any) -> Action = { payload ->
                payload as List<Post>
                dispatcher.dispatch(PostListApiSuccess(payload = payload))
                //= dispatcher.dispatch(UpdatePostList(payload = payload, apiState = ApiState.SUCCESS, dbState = DbState.UNCHANGED))
                DbAction.UpdateList(payload = payload,
                                    klass = Post::class,
                                    nextAction = { payload ->
                                        PostListSaveToDb()
                                        //= UpdatePostList(payload = null, apiState = ApiState.SUCCESS, dbState = DbState.SAVE)
                                    })
            }

            val onApiFail: (Any?) -> Action = { payload ->
                PostListApiFail()
                //= UpdatePostList(payload = null, apiState = ApiState.FAIL, dbState = DbState.UNCHANGED)
            }

            return ApiAction.RequestApi(payload = "query",
                                        onRequesting = onApiRequesting,
                                        onSuccess = onApiSuccess,
                                        onFail = onApiFail,
                                        responseType = ApiResponseType.POST_LIST)
        }
    }

    // general update post state
    class UpdatePostList(override val payload: List<Post>?,
                         val apiState: ApiState = ApiState.NONE,
                         val dbState: DbState = DbState.NONE) : PostAction()

    //endregion

    //================================================================================
    // region action from database
    //================================================================================

    class PostListQueryFromDb(override val payload: List<Post>?) : PostAction()
    class PostListSaveToDb() : PostAction()

    //endregion

    //================================================================================
    // region action from api
    //================================================================================

    class PostListApiRequesting() : PostAction()
    class PostListApiSuccess(override val payload: List<Post>?) : PostAction()
    class PostListApiFail() : PostAction()

    //endregion

}