package `in`.bitotsav.profile.ui


import `in`.bitotsav.R
import `in`.bitotsav.databinding.FragmentRegistrationBinding
import `in`.bitotsav.profile.data.RegistrationFields
import `in`.bitotsav.shared.utils.onTrue
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.sharedViewModel

class RegistrationFragment : Fragment() {

    private val registrationViewModel by sharedViewModel<RegistrationViewModel>()

    companion object {
        const val TAG = "RegF"
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "pass"
        const val KEY_STEP = "step"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        savedInstanceState?.getString(KEY_EMAIL)?.let {
            registrationViewModel.fields.email.text.value = it
        }
        savedInstanceState?.getString(KEY_PASSWORD)?.let {
            registrationViewModel.fields.password.text.value = it
        }
        savedInstanceState?.getInt(KEY_STEP)?.let {
            registrationViewModel.nextStep.value = it
        } ?: run {
            registrationViewModel.fields = RegistrationFields()
            Log.d(TAG, "Reset all fields")
        }
        registrationViewModel.mColor = TypedValue().apply {
            activity?.theme?.resolveAttribute(R.attr.colorPrimary, this, true)
        }.data

//            .mColor = context?.getColorCompat(R.color.colorRed) ?: 0xFF0000

        registrationViewModel.nextStep.observe(
            viewLifecycleOwner,
            Observer { nextStep ->
                if (nextStep == 4) {
                    // Go back to profile fragment after registration is complete.
                    findNavController().navigate(R.id.action_destRegistration_to_destProfile)
                }
            }
        )

        return FragmentRegistrationBinding.inflate(inflater, container, false)
            .apply {
                lifecycleOwner = this@RegistrationFragment
                viewModel = registrationViewModel
                with(registrationViewModel.nextStep) {
                    ObjectAnimator.ofInt(progress, "progress", ((value * 100) / 3) + 1)
                        .apply {
                            duration = 500
                            start()
                        }
                    observe(viewLifecycleOwner, Observer {
                        ObjectAnimator.ofInt(progress, "progress", ((it * 100) / 3) + 1)
                            .apply {
                                duration = 500
                                start()
                            }
                    })
                }
            }
            .root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // TODO [Refactor]: Figure out if saving email and password is necessary
        registrationViewModel.fields.email.text.value.let {
            it.isNotEmpty().onTrue { outState.putString(KEY_EMAIL, it) }
        }
        registrationViewModel.fields.password.text.value.let {
            it.isNotEmpty().onTrue { outState.putString(KEY_PASSWORD, it) }
        }
        outState.putInt(KEY_STEP, registrationViewModel.nextStep.value)
        Log.d(TAG, "Saved email and password state")
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        with(registrationViewModel) {
            waiting.value = false
            allDone.value = false
            loggedIn.value = false
            registrationError.value = ""
            nextStep.value = 1
        }
        super.onDestroyView()
    }

}
