package biz.dealnote.messenger.mvp.presenter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import biz.dealnote.messenger.domain.IAudioInteractor;
import biz.dealnote.messenger.domain.InteractorFactory;
import biz.dealnote.messenger.model.Audio;
import biz.dealnote.messenger.mvp.presenter.base.AccountDependencyPresenter;
import biz.dealnote.messenger.mvp.view.IAudiosView;
import biz.dealnote.messenger.place.PlaceFactory;
import biz.dealnote.messenger.player.MusicPlaybackService;
import biz.dealnote.messenger.util.RxUtils;
import biz.dealnote.messenger.util.Utils;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by admin on 1/4/2018.
 * Phoenix-for-VK
 */
public class AudiosPresenter extends AccountDependencyPresenter<IAudiosView> {

    private final IAudioInteractor audioInteractor;
    private final ArrayList<Audio> audios;
    private final int ownerId;
    private final boolean audioAvailable;

    public AudiosPresenter(int accountId, int ownerId, @Nullable Bundle savedInstanceState) {
        super(accountId, savedInstanceState);
        this.audioInteractor = InteractorFactory.createAudioInteractor();
        this.audioAvailable = audioInteractor.isAudioPluginAvailable();
        this.audios = new ArrayList<>();
        this.ownerId = ownerId;

        if(audioAvailable){
            requestList();
        }
    }

    private CompositeDisposable audioListDisposable = new CompositeDisposable();

    private void requestList() {
        audioListDisposable.add(audioInteractor.get(ownerId, 0)
                .compose(RxUtils.applySingleIOToMainSchedulers())
                .subscribe(this::onListReceived, this::onListGetError));
    }

    private void onListReceived(List<Audio> data) {
        audios.clear();
        audios.addAll(data);
        callView(IAudiosView::notifyListChanged);
    }

    public void playAudio(Context context, int position){
        MusicPlaybackService.startForPlayList(context, audios, position, false);
        PlaceFactory.getPlayerPlace(getAccountId()).tryOpenWith(context);
    }

    public void fireRefresh(){
        if(audioAvailable){
            audioListDisposable.clear();
            requestList();
        }
    }

    @Override
    public void onDestroyed() {
        audioListDisposable.dispose();
        super.onDestroyed();
    }

    private void onListGetError(Throwable t) {
        showError(getView(), Utils.getCauseIfRuntime(t));
    }

    @Override
    public void onGuiCreated(@NonNull IAudiosView view) {
        super.onGuiCreated(view);
        view.displayList(audios);
        view.setBlockedScreen(!audioAvailable);
    }

    @Override
    protected String tag() {
        return AudiosPresenter.class.getSimpleName();
    }
}